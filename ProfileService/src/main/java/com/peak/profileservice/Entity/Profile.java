package com.peak.profileservice.Entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "profile_table")
public class Profile {

    @Id
    @Column(name = "user_id", nullable = false)
    @NotNull
    @NotBlank
    private String userId;

    @Column(name = "profile_pic_url", nullable = true)
    private String profile_pic_url;

    @Column(name = "bio", nullable = true)
    private String bio;

    @Column(name = "profile_name", nullable = false, unique = true)
    @NotNull(message = "Profile name cannot be null")
    @NotBlank(message = "Profile name cannot be blank")
    private String profileName;

    @NotNull(message = "Privacy cannot be null")
    @NotBlank(message = "Privacy cannot be blank")
    private Privacy privacy=Privacy.PUBLIC;
}
