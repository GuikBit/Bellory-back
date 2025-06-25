package org.exemplo.bellory.model.entity.config;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configSistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int organizacao_id;


}
