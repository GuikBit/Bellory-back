package org.exemplo.bellory.model.dto;

import java.math.BigDecimal;
import java.util.List;

public interface ServicoAgendamento {
    Long getId();
    String getNome();
    BigDecimal getPreco();
    List<String> getUrlsImagens();
}
