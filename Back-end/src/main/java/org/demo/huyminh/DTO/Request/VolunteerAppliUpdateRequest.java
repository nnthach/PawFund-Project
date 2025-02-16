package org.demo.huyminh.DTO.Request;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.demo.huyminh.Entity.User;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VolunteerAppliUpdateRequest {

    String fullName;
    int yob;
    String gender;
    String address;
    @Pattern(regexp = "(84|0[3|5|7|8|9])+(\\d{8})\\b")
    String phone;
    String adoptionExp;

    String daysOfWeek;
    String morning;
    String afternoon;
    String reason;

    int status;

    @Column(name = "update_Status_at")
    private LocalDateTime updateAt;
}
