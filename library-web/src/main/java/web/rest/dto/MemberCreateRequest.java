package web.rest.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest {

    @NotBlank(message = "The membership number is required")
    @Size(max = 20, message = "The membership number must be less than 20 characters")
    private String membershipNumber;

    @NotBlank(message = "The first name is required")
    @Size(max = 100, message = "The first name must be less than 100 characters")
    private String firstName;

    @NotBlank(message = "The last name is required")
    @Size(max = 100, message = "The last name must be less than 100 characters")
    private String lastName;

    @NotBlank(message = "The email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "The phone must be less than 20 characters")
    private String phone;

    @Size(max = 255, message = "The address must be less than 255 characters")
    private String address;

    @Min(value = 1, message = "The max loans must be at least 1")
    private int maxLoans = 5;
}
