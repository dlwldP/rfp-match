package com.rfpmatch.requirement.dto;

import com.rfpmatch.requirement.domain.RequirementItem;

public record RequirementItemResponse(
        Long requirementId,
        String category,
        String description,
        String requiredValue,
        String unit,
        boolean mandatory) {

    public static RequirementItemResponse from(RequirementItem item) {
        return new RequirementItemResponse(
                item.getId(),
                item.getCategory(),
                item.getDescription(),
                item.getRequiredValue(),
                item.getUnit(),
                item.isMandatory());
    }
}
