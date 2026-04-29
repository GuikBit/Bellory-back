package org.exemplo.bellory.service.questionario;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.questionario.Pergunta;
import org.exemplo.bellory.model.entity.questionario.RespostaPergunta;
import org.exemplo.bellory.model.entity.questionario.RespostaQuestionario;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.service.ArquivoStorageService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera comprovante PDF de termo de consentimento + assinatura(s) digital(is).
 *
 * Implementacao com PDFBox 3.0.x. Em fase 1 o conteudo do termo eh renderizado como
 * texto plano (Markdown basico convertido) sem render completo. A imagem PNG/SVG das
 * assinaturas eh embutida (apenas PNG suportado em fase 1; SVG ignorado por enquanto).
 */
@Service
public class ComprovanteTermoPdfService {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 14f;
    private static final float SECTION_GAP = 18f;

    private static final float TITLE_FONT_SIZE = 16f;
    private static final float HEADER_FONT_SIZE = 12f;
    private static final float BODY_FONT_SIZE = 11f;
    private static final float META_FONT_SIZE = 9f;

    private static final float ASSINATURA_MAX_WIDTH = 280f;
    private static final float ASSINATURA_MAX_HEIGHT = 100f;

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ArquivoStorageService arquivoStorageService;

    public ComprovanteTermoPdfService(ArquivoStorageService arquivoStorageService) {
        this.arquivoStorageService = arquivoStorageService;
    }

    public byte[] gerarPdf(RespostaQuestionario resposta) {
        Long organizacaoId = resposta.getQuestionario() != null
                && resposta.getQuestionario().getOrganizacao() != null
                ? resposta.getQuestionario().getOrganizacao().getId() : null;

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            Cursor cursor = new Cursor(doc);
            cursor.newPage();

            renderHeader(cursor, resposta, fontBold, fontRegular);
            renderBody(cursor, resposta, organizacaoId, doc, fontRegular, fontBold);
            renderFooter(cursor, fontRegular);

            cursor.close();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar PDF do comprovante: " + e.getMessage(), e);
        }
    }

    // ===== Secoes =====

    private void renderHeader(Cursor c, RespostaQuestionario resposta, PDFont fontBold, PDFont fontRegular) throws IOException {
        Organizacao org = resposta.getQuestionario() != null
                ? resposta.getQuestionario().getOrganizacao() : null;

        String tituloEstabelecimento = org != null && org.getNomeFantasia() != null
                ? org.getNomeFantasia() : "Estabelecimento";
        c.drawText(tituloEstabelecimento, fontBold, TITLE_FONT_SIZE);
        c.advance(LINE_HEIGHT);

        if (org != null && org.getCnpj() != null) {
            c.drawText("CNPJ: " + org.getCnpj(), fontRegular, META_FONT_SIZE);
            c.advance(LINE_HEIGHT - 2);
        }

        String tituloQuestionario = resposta.getQuestionario() != null
                ? resposta.getQuestionario().getTitulo() : "Questionário";
        c.drawText("Comprovante: " + tituloQuestionario, fontBold, HEADER_FONT_SIZE);
        c.advance(LINE_HEIGHT);

        if (resposta.getDtResposta() != null) {
            c.drawText("Data: " + DATA_HORA.format(resposta.getDtResposta()), fontRegular, META_FONT_SIZE);
            c.advance(LINE_HEIGHT - 2);
        }
        if (resposta.getIpOrigem() != null) {
            c.drawText("IP de origem: " + resposta.getIpOrigem(), fontRegular, META_FONT_SIZE);
            c.advance(LINE_HEIGHT - 2);
        }
        if (resposta.getDispositivo() != null) {
            c.drawText("Dispositivo: " + resposta.getDispositivo(), fontRegular, META_FONT_SIZE);
            c.advance(LINE_HEIGHT - 2);
        }

        c.advance(SECTION_GAP);
        c.drawHorizontalLine();
        c.advance(SECTION_GAP);
    }

    private void renderBody(Cursor c, RespostaQuestionario resposta, Long organizacaoId,
                            PDDocument doc, PDFont fontRegular, PDFont fontBold) throws IOException {
        if (resposta.getRespostas() == null) return;

        for (RespostaPergunta rp : resposta.getRespostas()) {
            Pergunta p = rp.getPergunta();
            if (p == null) continue;
            TipoPergunta tipo = p.getTipo();

            if (tipo == TipoPergunta.TERMO_CONSENTIMENTO) {
                renderTermo(c, rp, p, fontRegular, fontBold);
                c.advance(SECTION_GAP);
            } else if (tipo == TipoPergunta.ASSINATURA) {
                renderAssinatura(c, rp, p, organizacaoId, doc, fontRegular, fontBold);
                c.advance(SECTION_GAP);
            }
        }
    }

    private void renderTermo(Cursor c, RespostaPergunta rp, Pergunta p,
                             PDFont fontRegular, PDFont fontBold) throws IOException {
        c.drawText(p.getTexto() != null ? p.getTexto() : "Termo de Consentimento",
                fontBold, HEADER_FONT_SIZE);
        c.advance(LINE_HEIGHT);

        String conteudo = rp.getTextoTermoRenderizado();
        if (conteudo != null && !conteudo.isBlank()) {
            String textoPlano = markdownParaTextoPlano(conteudo);
            renderTextoComWrap(c, textoPlano, fontRegular, BODY_FONT_SIZE);
        }

        c.advance(LINE_HEIGHT / 2);
        renderLinhaAuditoriaTermo(c, rp, fontRegular);
    }

    private void renderAssinatura(Cursor c, RespostaPergunta rp, Pergunta p,
                                  Long organizacaoId, PDDocument doc,
                                  PDFont fontRegular, PDFont fontBold) throws IOException {
        c.drawText(p.getTexto() != null ? p.getTexto() : "Assinatura Digital",
                fontBold, HEADER_FONT_SIZE);
        c.advance(LINE_HEIGHT);

        if (rp.getArquivoAssinaturaClienteId() != null) {
            c.drawText("Cliente:", fontRegular, BODY_FONT_SIZE);
            c.advance(LINE_HEIGHT);
            embedAssinatura(c, doc, rp.getArquivoAssinaturaClienteId(), organizacaoId);
        }

        if (rp.getArquivoAssinaturaProfissionalId() != null) {
            c.advance(LINE_HEIGHT);
            c.drawText("Profissional responsável:", fontRegular, BODY_FONT_SIZE);
            c.advance(LINE_HEIGHT);
            embedAssinatura(c, doc, rp.getArquivoAssinaturaProfissionalId(), organizacaoId);
        }
    }

    private void embedAssinatura(Cursor c, PDDocument doc, Long arquivoId, Long organizacaoId) throws IOException {
        if (arquivoId == null || organizacaoId == null) return;
        byte[] bytes;
        try {
            bytes = arquivoStorageService.lerAssinatura(arquivoId, organizacaoId);
        } catch (RuntimeException ex) {
            c.drawText("[Assinatura indisponivel: " + ex.getMessage() + "]",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), META_FONT_SIZE);
            c.advance(LINE_HEIGHT);
            return;
        }

        // PDFBox so suporta PNG/JPEG; SVG nao eh embutido em fase 1.
        if (!isPng(bytes)) {
            c.drawText("[Assinatura em formato nao suportado para PDF]",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), META_FONT_SIZE);
            c.advance(LINE_HEIGHT);
            return;
        }

        PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "assinatura");
        float ratio = (float) img.getWidth() / (float) img.getHeight();
        float width = ASSINATURA_MAX_WIDTH;
        float height = width / ratio;
        if (height > ASSINATURA_MAX_HEIGHT) {
            height = ASSINATURA_MAX_HEIGHT;
            width = height * ratio;
        }

        c.ensureSpace(height + LINE_HEIGHT);
        float yImg = c.getY() - height;
        c.getStream().drawImage(img, MARGIN, yImg, width, height);
        c.setY(yImg);
        c.advance(LINE_HEIGHT / 2);
    }

    private void renderLinhaAuditoriaTermo(Cursor c, RespostaPergunta rp, PDFont fontRegular) throws IOException {
        StringBuilder sb = new StringBuilder("- ");
        if (Boolean.TRUE.equals(rp.getAceitouTermo())) {
            sb.append("Aceito ");
        } else {
            sb.append("Não aceito ");
        }
        if (rp.getDataAceite() != null) {
            sb.append("em ").append(DATA_HORA.format(rp.getDataAceite())).append(" ");
        }
        if (rp.getHashTermo() != null) {
            sb.append("· Hash: ").append(rp.getHashTermo());
        }
        c.drawText(sb.toString(), fontRegular, META_FONT_SIZE);
        c.advance(LINE_HEIGHT - 2);
    }

    private void renderFooter(Cursor c, PDFont fontRegular) throws IOException {
        c.advance(SECTION_GAP);
        c.drawHorizontalLine();
        c.advance(LINE_HEIGHT);
        c.drawText("Documento gerado eletronicamente pelo sistema Bellory.",
                fontRegular, META_FONT_SIZE);
    }

    // ===== Word wrap =====

    private void renderTextoComWrap(Cursor c, String texto, PDFont fonte, float fontSize) throws IOException {
        float maxWidth = PDRectangle.A4.getWidth() - 2 * MARGIN;
        for (String paragrafo : texto.split("\n", -1)) {
            if (paragrafo.isBlank()) {
                c.advance(LINE_HEIGHT / 2);
                continue;
            }
            for (String linha : wrap(paragrafo, fonte, fontSize, maxWidth)) {
                c.drawText(linha, fonte, fontSize);
                c.advance(LINE_HEIGHT);
            }
        }
    }

    private List<String> wrap(String texto, PDFont fonte, float fontSize, float maxWidth) throws IOException {
        List<String> resultado = new ArrayList<>();
        String[] palavras = texto.split(" ");
        StringBuilder atual = new StringBuilder();

        for (String palavra : palavras) {
            String tentativa = atual.length() == 0 ? palavra : atual + " " + palavra;
            float largura = fonte.getStringWidth(sanitize(tentativa)) / 1000f * fontSize;
            if (largura > maxWidth && atual.length() > 0) {
                resultado.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual.setLength(0);
                atual.append(tentativa);
            }
        }
        if (atual.length() > 0) {
            resultado.add(atual.toString());
        }
        return resultado;
    }

    /**
     * Remove caracteres que Helvetica (WinAnsi) nao suporta para evitar IllegalArgumentException
     * no {@code getStringWidth}. Substitui chars problematicos por '?'.
     */
    private String sanitize(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch < 0x20 && ch != '\n' && ch != '\t') {
                sb.append('?');
            } else if (ch > 0xFF) {
                sb.append('?');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Conversao Markdown basico -> texto plano. Em fase 1 mantem sem formatacao rica.
     */
    private String markdownParaTextoPlano(String md) {
        if (md == null) return "";
        String s = md;
        s = s.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        s = s.replaceAll("__(.+?)__", "$1");
        s = s.replaceAll("\\*(.+?)\\*", "$1");
        s = s.replaceAll("_(.+?)_", "$1");
        s = s.replaceAll("(?m)^#{1,6}\\s*", "");
        s = s.replaceAll("(?m)^\\s*[-*+]\\s+", "- ");
        return s;
    }

    private boolean isPng(byte[] bytes) {
        return bytes != null && bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A
                && bytes[6] == 0x1A && bytes[7] == 0x0A;
    }

    // ===== Cursor: encapsula posicao e quebra de pagina =====

    private static class Cursor {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        Cursor(PDDocument doc) {
            this.doc = doc;
        }

        void newPage() throws IOException {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        void advance(float pts) throws IOException {
            y -= pts;
            ensureSpace(0);
        }

        void ensureSpace(float requiredAhead) throws IOException {
            if (y - requiredAhead < MARGIN) {
                newPage();
            }
        }

        void drawText(String text, PDFont fonte, float fontSize) throws IOException {
            String sanitizado = sanitizeText(text);
            stream.beginText();
            stream.setFont(fonte, fontSize);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitizado);
            stream.endText();
        }

        void drawHorizontalLine() throws IOException {
            stream.moveTo(MARGIN, y);
            stream.lineTo(PDRectangle.A4.getWidth() - MARGIN, y);
            stream.stroke();
        }

        PDPageContentStream getStream() { return stream; }

        float getY() { return y; }

        void setY(float y) { this.y = y; }

        void close() throws IOException {
            if (stream != null) stream.close();
        }

        private static String sanitizeText(String input) {
            if (input == null) return "";
            StringBuilder sb = new StringBuilder(input.length());
            for (int i = 0; i < input.length(); i++) {
                char ch = input.charAt(i);
                if (ch < 0x20 && ch != '\t') {
                    sb.append(' ');
                } else if (ch > 0xFF) {
                    sb.append('?');
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
    }
}
