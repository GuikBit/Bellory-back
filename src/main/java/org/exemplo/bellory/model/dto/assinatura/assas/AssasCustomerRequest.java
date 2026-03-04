package org.exemplo.bellory.model.dto.assinatura.assas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssasCustomerRequest {
    private String name;
    private String cpfCnpj;
    private String email;
    private String phone;
}
