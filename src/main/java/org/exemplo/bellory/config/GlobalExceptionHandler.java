package org.exemplo.bellory.config;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.exception.LimitePlanoExcedidoException;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice // Combina @ControllerAdvice e @ResponseBody. Captura exceções de todos os @RestControllers.
public class GlobalExceptionHandler {

    /**
     * Captura os erros de validação do @Valid e formata a resposta.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Define o status HTTP da resposta
    public ResponseAPI<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Cria um mapa para armazenar os erros (campo -> mensagem)
        Map<String, String> errors = new HashMap<>();

        // Itera sobre todos os erros de campo da exceção
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Retorna a sua estrutura de resposta padrão
        return ResponseAPI.builder()
                .success(false)
                .message("Erro de validação.")
                .errorCode(400)
                .errors(errors) // Adicionei um campo 'errors' no seu ResponseAPI
                .build();
    }

    /**
     * Captura as exceções de regra de negócio (lançadas do Service).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseAPI<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseAPI.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(400)
                .build();
    }

    /**
     * Captura estouro de limite de plano e retorna 422 com payload da violacao.
     */
    @ExceptionHandler(LimitePlanoExcedidoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseAPI<Map<String, Object>> handleLimitePlanoExcedido(LimitePlanoExcedidoException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("limitKey", ex.getLimitKey());
        if (ex.getLimiteMaximo() != null) details.put("limiteMaximo", ex.getLimiteMaximo());
        if (ex.getUsoAtual() != null) details.put("usoAtual", ex.getUsoAtual());
        log.info("Limite plano excedido: {} (key={}, maximo={}, atual={})",
                ex.getMessage(), ex.getLimitKey(), ex.getLimiteMaximo(), ex.getUsoAtual());
        return ResponseAPI.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(422)
                .dados(details)
                .build();
    }

    /**
     * Captura qualquer outra exceção não tratada para evitar expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseAPI<Object> handleGeneralExceptions(Exception ex) {
        log.error("Erro não esperado: ", ex);
        return ResponseAPI.builder()
                .success(false)
                .message("Ocorreu um erro interno no servidor.")
                .errorCode(500)
                .build();
    }
}
