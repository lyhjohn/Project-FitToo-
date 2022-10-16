package com.fittoo.common.entity;

import com.fittoo.member.model.LoginType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder
public abstract class BaseEntity {

    private String userId;
    private String password;
    private String phoneNumber;
    private String gender; // 1=남자 2=여자

    @Enumerated(EnumType.STRING)
    private LoginType loginType;

    private String exercisePeriod;



}
