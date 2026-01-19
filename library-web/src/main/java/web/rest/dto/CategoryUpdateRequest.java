package web.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {

    @NotBlank(message = "The name is required")
    @Size(max = 100, message = "The name must be less than 100 characters")
    private String name;

    @Size(max = 500, message = "The description must be less than 500 characters")
    private String description;
}
