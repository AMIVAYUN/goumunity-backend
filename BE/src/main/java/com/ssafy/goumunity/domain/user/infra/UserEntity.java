package com.ssafy.goumunity.domain.user.infra;

import com.ssafy.goumunity.domain.user.domain.User;
import com.ssafy.goumunity.domain.user.domain.UserCategory;
import com.ssafy.goumunity.domain.user.domain.UserStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "month_budget")
    private Long monthBudget;

    @Column(name = "age")
    private Integer age;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private UserCategory userCategory;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "img_src")
    private String imgSrc;

    @Column(name = "register_date")
    private Instant registerDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Column(name = "last_password_modified_date")
    private Instant lastPasswordModifiedDate;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static UserEntity fromModel(User user) {

        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .monthBudget(user.getMonthBudget())
                .age(user.getAge())
                .userCategory(user.getUserCategory())
                .gender(user.getGender())
                .nickname(user.getNickname())
                .imgSrc(user.getImgSrc())
                .registerDate(user.getRegisterDate())
                .userStatus(user.getUserStatus())
                .lastPasswordModifiedDate(user.getLastPasswordModifiedDate())
                .regionId(user.getRegionId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toModel() {
        return User.builder()
                .id(this.id)
                .email(this.email)
                .password(this.password)
                .monthBudget(this.monthBudget)
                .age(this.age)
                .userCategory(this.userCategory)
                .gender(this.gender)
                .nickname(this.nickname)
                .imgSrc(this.imgSrc)
                .registerDate(this.registerDate)
                .userStatus(this.userStatus)
                .lastPasswordModifiedDate(this.lastPasswordModifiedDate)
                .regionId(this.regionId)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static UserEntity userEntityOnlyWithId(Long id) {
        return UserEntity.builder().id(id).build();
    }
}
