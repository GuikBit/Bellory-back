package org.exemplo.bellory.config;

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
     * Captura qualquer outra exceção não tratada para evitar expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseAPI<Object> handleGeneralExceptions(Exception ex) {
        // Opcional: logar a exceção completa para depuração
        // log.error("Ocorreu um erro não esperado: ", ex);
        return ResponseAPI.builder()
                .success(false)
                .message("Ocorreu um erro interno no servidor.")
                .errorCode(500)
                .build();
    }
}
