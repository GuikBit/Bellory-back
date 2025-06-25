package org.exemplo.bellory.model.entity.pagamento;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cobranca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
