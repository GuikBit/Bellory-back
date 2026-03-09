package org.exemplo.bellory.model.dto.assinatura.assas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssasPaymentListResponse {
    private List<AssasPaymentResponse> data;
    private boolean hasMore;
    private int totalCount;
    private int limit;
    private int offset;
}
