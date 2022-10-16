package com.fittoo.trainer.entity;

import com.fittoo.common.entity.BaseEntity;
import com.fittoo.member.model.LoginType;
import com.fittoo.trainer.model.TrainerInput;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Trainer extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;
    private long price;
    private String awards;
    private String mainPtList;
    private String introduce;
    private String profilePictureNewName;
    private String profilePictureOriName;


    public static String setMainPtList(List<String> ptList) {
        StringBuilder sb = new StringBuilder();
        int length = ptList.size();
        for (String pt : ptList) {
            if (--length == 0) {
                sb.append(pt);
            } else {
                sb.append(pt).append(",");
            }
        }
        return sb.toString();
    }

    public static Trainer of(TrainerInput trainerInput, String[] fileNames) {
        return Trainer.builder()
                .userId(trainerInput.getUserId())
                .password(trainerInput.getPassword())
                .phoneNumber(trainerInput.getPhoneNumber())
                .loginType(LoginType.TRAINER)
                .awards(trainerInput.getAwards())
                .mainPtList(setMainPtList(trainerInput.getMainPtList()))
                .price(trainerInput.getPrice())
                .exercisePeriod(trainerInput.getExercisePeriod())
                .gender(setGender(trainerInput.getGender()))
                .introduce(trainerInput.getIntroduce())
                .profilePictureOriName(fileNames[0])
                .profilePictureNewName(fileNames[1])
                .build();
    }

    public static String setGender(int num) {
        switch (num) {
            case 1:
                return "남자";
            case 2:
                return "여자";
        }
        return null;
    }
}
