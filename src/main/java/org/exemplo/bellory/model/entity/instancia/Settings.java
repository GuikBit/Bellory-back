package org.exemplo.bellory.model.entity.instancia;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instance_settings", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean rejectCall;
    private String msgCall;
    private Boolean groupsIgnore;
    private Boolean alwaysOnline;
    private Boolean readMessages;
    private Boolean readStatus;

    // ✅ NOVO: Campo usado pelo frontend para rejeição de horário
    @Column(name = "out_of_hours")
    private Boolean outOfHours;
}