package com.filmexa.stream.modules.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One selectable quality in the player's menu. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantDto {

    private String label;
    private int height;
    private int width;
    private int bandwidth;
    private String url;
}
