package org.exemplo.bellory.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoDTO {

    private String country;
    private String state;
    private String city;
}
