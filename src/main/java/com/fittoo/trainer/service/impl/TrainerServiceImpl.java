package com.fittoo.trainer.service.impl;

import static com.fittoo.common.message.FileErrorMessage.INVALID_FILE;
import static com.fittoo.common.message.FileErrorMessage.INVALID_PROFILE_PICTURE;
import static com.fittoo.common.message.FindErrorMessage.NOT_FOUND_TRAINER;
import static com.fittoo.common.message.RegisterErrorMessage.ALREADY_EXIST_USERID;
import static com.fittoo.common.message.RegisterErrorMessage.Pwd_And_RePwd_Not_Equal;
import static com.fittoo.common.message.ScheduleErrorMessage.CONTAINS_REGISTERED_DATE;
import static com.fittoo.common.message.ScheduleErrorMessage.START_DAY_BIGGER_THAN_END_DAY;
import static com.fittoo.common.message.ScheduleErrorMessage.START_TIME_BIGGER_THAN_END_TIME;
import static com.fittoo.trainer.entity.QTrainer.trainer;
import static com.fittoo.utills.CalendarUtil.StringToLocalTime.getEndTime;
import static com.fittoo.utills.CalendarUtil.StringToLocalTime.getStartTime;

import com.fittoo.exception.FileException;
import com.fittoo.exception.RegisterException;
import com.fittoo.exception.ScheduleException;
import com.fittoo.exception.UserIdAlreadyExist;
import com.fittoo.exception.UserNotFoundException;
import com.fittoo.trainer.entity.ExerciseType;
import com.fittoo.trainer.entity.QTrainer;
import com.fittoo.trainer.entity.Schedule;
import com.fittoo.trainer.entity.Trainer;
import com.fittoo.trainer.model.ScheduleDto;
import com.fittoo.trainer.model.ScheduleInput;
import com.fittoo.trainer.model.TrainerDto;
import com.fittoo.trainer.model.TrainerInput;
import com.fittoo.trainer.model.UpdateInput;
import com.fittoo.trainer.repository.ExerciseTypeRepository;
import com.fittoo.trainer.repository.ScheduleRepository;
import com.fittoo.trainer.repository.TrainerRepository;
import com.fittoo.trainer.service.TrainerService;
import com.fittoo.utills.CalendarUtil.StringToLocalDate;
import com.fittoo.utills.FileStore;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TrainerServiceImpl implements TrainerService {

	private final TrainerRepository trainerRepository;
	private final ScheduleRepository scheduleRepository;
	private final ExerciseTypeRepository exerciseTypeRepository;
	private final JPAQueryFactory queryFactory;

	@Override
	@Transactional
	public void trainerRegister(TrainerInput input) {
		Optional<Trainer> optionalTrainer = trainerRepository.findByUserId(
			input.getUserId());

		if (optionalTrainer.isPresent()) {
			input.setLoginType("trainer");
			throw new RegisterException(ALREADY_EXIST_USERID.message(), input, input.getLoginType(),
				new UserIdAlreadyExist());
		}

		if (!input.getPassword().equals(input.getRepassword())) {
			input.setLoginType("trainer");
			throw new RegisterException(Pwd_And_RePwd_Not_Equal.message(), input,
				input.getLoginType());
		}

		String[] fileNames;
		try {
			fileNames = new FileStore().storeFile(input.getProfilePicture(), "trainer");
		} catch (IOException e) {
			throw new RegisterException(INVALID_FILE.message(), new FileException());
		}

		String encPassword = BCrypt.hashpw(input.getPassword(), BCrypt.gensalt());
		input.setPassword(encPassword);
		Trainer trainer = Trainer.of(input, fileNames);

		Optional<ExerciseType> optionalExerciseType = exerciseTypeRepository.findById(
			input.getExerciseType());

		trainerRepository.save(trainer);

		if (optionalExerciseType.isEmpty()) {
			ExerciseType exerciseType = exerciseTypeRepository.save(
				new ExerciseType(input.getExerciseType()));
			exerciseType.addTrainer(trainer);
			exerciseTypeRepository.save(exerciseType);
		} else {
			ExerciseType exerciseType = optionalExerciseType.get();
			exerciseType.addTrainer(trainer);
			exerciseTypeRepository.save(exerciseType);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public TrainerDto findTrainer(String userId) {
		Optional<Trainer> optionalTrainer = trainerRepository.findByUserId(userId);

		return optionalTrainer.map(TrainerDto::of).orElseThrow(()
			-> new UserNotFoundException(NOT_FOUND_TRAINER.message()));
	}

	@Override
	@Transactional
	public TrainerDto update(UpdateInput input) {
		Optional<Trainer> optionalTrainer = trainerRepository.findByUserId(input.getUserId());

		return optionalTrainer.map(x -> TrainerDto.of(x.update(input)))
			.orElseThrow(() -> new UserNotFoundException(NOT_FOUND_TRAINER.message()));
	}

	@Override
	@Transactional
	public TrainerDto updateProfilePicture(MultipartFile file, String userId) {
		Optional<Trainer> optionalTrainer = trainerRepository.findByUserId(userId);
		if (optionalTrainer.isEmpty()) {
			return null;
		}
		Trainer trainer = optionalTrainer.get();

		String[] fileNames;
		try {
			fileNames = new FileStore().storeFile(file, "trainer");
		} catch (IOException e) {
			throw new FileException(INVALID_PROFILE_PICTURE.message());
		}
		trainer.updateProfilePicture(fileNames);

		return TrainerDto.of(trainer);
	}

	@Override
	@Transactional
	public List<TrainerDto> findTrainersPerPage(int page) {
		PageRequest pageRequest = PageRequest.of(page - 1, 5, Direction.ASC, "userName");

		List<Trainer> trainerList = queryFactory
			.selectFrom(trainer)
			.offset(pageRequest.getOffset())
			.limit(pageRequest.getPageSize())
			.fetch();


		return TrainerDto.of(trainerList);
	}

	@Override
	public Long getTotalCountTrainerList() {
		return queryFactory
			.select(Wildcard.count)
			.from(trainer)
			.fetchOne();
	}

	@Override
	@Transactional
	public Optional<List<ScheduleDto>> showSchedule(String userId) {
		Optional<Trainer> optionalTrainer = trainerRepository.findByUserId(userId);
		if (optionalTrainer.isEmpty()) {
			return Optional.empty();
		}
		Trainer trainer = optionalTrainer.get();
		return Optional.ofNullable(ScheduleDto.of(trainer.getScheduleList()));
	}

	@Override
	@Transactional
	public void createSchedule(String userId, ScheduleInput input) throws ParseException {
		if (!isStartDateIsBeforeEndDate(input)) {
			throw new ScheduleException(START_DAY_BIGGER_THAN_END_DAY.message());
		}

		if (!isStartTimeIsBeforeEndTime(input)) {
			throw new ScheduleException(START_TIME_BIGGER_THAN_END_TIME.message());
		}

		Optional<Trainer> optionalTrainer = trainerRepository.findByUserId(userId);
		if (optionalTrainer.isEmpty()) {
			throw new UserNotFoundException(NOT_FOUND_TRAINER.message());
		}

		Trainer trainer = optionalTrainer.get();
		LocalDate startDate = StringToLocalDate.getStartDate(input.getStartDate());
		LocalDate endDate = StringToLocalDate.getEndDate(input.getEndDate());

		Optional<List<Schedule>> optionalScheduleList = scheduleRepository.findAllByTrainerUserIdAndDateBetween(
			trainer.getUserId(), startDate, endDate);

		if (optionalScheduleList.isPresent()) {
			if (!CollectionUtils.isEmpty(optionalScheduleList.get())) {
				throw new ScheduleException(CONTAINS_REGISTERED_DATE.message());
			}
		}

		List<Schedule> scheduleList = trainer.setSchedule(input, trainer.getUserId());
		scheduleRepository.saveAll(scheduleList);
	}

	private static boolean isStartDateIsBeforeEndDate(ScheduleInput input) throws ParseException {
		if (input.getStartDate().equals(input.getEndDate())) {
			return true;
		}

		return StringToLocalDate.getStartDate(input.getStartDate())
			.isBefore(StringToLocalDate.getEndDate(input.getEndDate()));
	}

	private static boolean isStartTimeIsBeforeEndTime(ScheduleInput input) throws ParseException {
		return getStartTime(input.getStartTime()).isBefore(getEndTime(input.getEndTime()));
	}
}
