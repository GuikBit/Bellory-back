package org.exemplo.bellory.model.entity.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Gera getters, setters, toString, equals, hashCode (Lombok)
@NoArgsConstructor // Construtor sem argumentos (Lombok)
@AllArgsConstructor // Construtor com todos os argumentos (Lombok)
@Builder // Padrão Builder para construção de objetos (Lombok)
@JsonInclude(JsonInclude.Include.NON_NULL) // Inclui apenas campos não nulos no JSON
public class ResponseAPI<T> {
    private boolean success;
    private String message;
    private T dados;
    private Integer errorCode; // Opcional, para códigos de erro específicos da aplicação
}
