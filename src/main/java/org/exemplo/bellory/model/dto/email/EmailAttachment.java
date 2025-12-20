package org.exemplo.bellory.model.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class EmailAttachment {
    private String filename;
    private byte[] content;
    private String contentType;
}
