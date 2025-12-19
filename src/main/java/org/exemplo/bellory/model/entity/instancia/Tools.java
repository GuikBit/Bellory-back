package org.exemplo.bellory.model.entity.instancia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instance_tools", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tools {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "get_services")
    private boolean getServices;

    @Column(name = "get_professional")
    private boolean getProfessional;

    @Column(name = "get_products")
    private boolean getProducts;

    @Column(name = "get_avaliable_schedules")
    private boolean getAvaliableSchedules;

    @Column(name = "post_scheduling")
    private boolean postScheduling;

    @Column(name = "send_text_message")
    private boolean sendTextMessage;

    @Column(name = "send_media_message")
    private boolean sendMediaMessage;

    @Column(name = "post_confirmations")
    private boolean postConfirmations;

    @Column(name = "post_cancellations")
    private boolean postCancellations;
}
