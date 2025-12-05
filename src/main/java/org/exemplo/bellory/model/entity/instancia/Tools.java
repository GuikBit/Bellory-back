package org.exemplo.bellory.model.entity.instancia;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instance_tools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tools {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean getServices;

    private boolean getProfessional;

    private boolean getProducts;

    private boolean getAvaliableSchedules;

    private boolean postScheduling;

    private boolean sendTextMessage;

    private boolean sendMediaMessage;

    private boolean postConfirmations;

    private boolean postCancellations;

}
