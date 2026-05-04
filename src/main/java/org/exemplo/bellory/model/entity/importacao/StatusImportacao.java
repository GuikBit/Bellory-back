package org.exemplo.bellory.model.entity.importacao;

public enum StatusImportacao {
    PENDENTE,      // criada, ainda nao iniciou processamento
    PROCESSANDO,   // worker async esta consumindo o CSV
    CONCLUIDO,     // chegou ao fim (com ou sem linhas ignoradas)
    FALHA          // erro fatal (parser quebrou, IO etc.) - importacao abortada
}
