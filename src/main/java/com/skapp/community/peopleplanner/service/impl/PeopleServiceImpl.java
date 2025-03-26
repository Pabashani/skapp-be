package com.skapp.community.peopleplanner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skapp.community.common.constant.CommonMessageConstant;
import com.skapp.community.common.exception.EntityNotFoundException;
import com.skapp.community.common.exception.ModuleException;
import com.skapp.community.common.exception.ValidationException;
import com.skapp.community.common.model.User;
import com.skapp.community.common.model.UserSettings;
import com.skapp.community.common.payload.response.BulkStatusSummary;
import com.skapp.community.common.payload.response.NotificationSettingsResponseDto;
import com.skapp.community.common.payload.response.PageDto;
import com.skapp.community.common.payload.response.ResponseEntityDto;
import com.skapp.community.common.repository.UserDao;
import com.skapp.community.common.service.BulkContextService;
import com.skapp.community.common.service.EncryptionDecryptionService;
import com.skapp.community.common.service.UserService;
import com.skapp.community.common.service.UserVersionService;
import com.skapp.community.common.service.impl.AsyncEmailServiceImpl;
import com.skapp.community.common.type.LoginMethod;
import com.skapp.community.common.type.NotificationSettingsType;
import com.skapp.community.common.type.Role;
import com.skapp.community.common.type.VersionType;
import com.skapp.community.common.util.CommonModuleUtils;
import com.skapp.community.common.util.DateTimeUtils;
import com.skapp.community.common.util.MessageUtil;
import com.skapp.community.common.util.Validation;
import com.skapp.community.common.util.event.UserCreatedEvent;
import com.skapp.community.common.util.event.UserDeactivatedEvent;
import com.skapp.community.common.util.transformer.PageTransformer;
import com.skapp.community.leaveplanner.type.ManagerType;
import com.skapp.community.peopleplanner.constant.PeopleConstants;
import com.skapp.community.peopleplanner.constant.PeopleMessageConstant;
import com.skapp.community.peopleplanner.mapper.PeopleMapper;
import com.skapp.community.peopleplanner.model.Employee;
import com.skapp.community.peopleplanner.model.EmployeeEducation;
import com.skapp.community.peopleplanner.model.EmployeeEmergency;
import com.skapp.community.peopleplanner.model.EmployeeFamily;
import com.skapp.community.peopleplanner.model.EmployeeManager;
import com.skapp.community.peopleplanner.model.EmployeePeriod;
import com.skapp.community.peopleplanner.model.EmployeePersonalInfo;
import com.skapp.community.peopleplanner.model.EmployeeProgression;
import com.skapp.community.peopleplanner.model.EmployeeRole;
import com.skapp.community.peopleplanner.model.EmployeeTeam;
import com.skapp.community.peopleplanner.model.EmployeeVisa;
import com.skapp.community.peopleplanner.model.JobFamily;
import com.skapp.community.peopleplanner.model.JobTitle;
import com.skapp.community.peopleplanner.model.Team;
import com.skapp.community.peopleplanner.payload.CurrentEmployeeDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeBulkDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeDataValidationDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeDetailsDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeExportFilterDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeFilterDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeProgressionsDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeQuickAddDto;
import com.skapp.community.peopleplanner.payload.request.EmployeeUpdateDto;
import com.skapp.community.peopleplanner.payload.request.NotificationSettingsPatchRequestDto;
import com.skapp.community.peopleplanner.payload.request.PermissionFilterDto;
import com.skapp.community.peopleplanner.payload.request.ProbationPeriodDto;
import com.skapp.community.peopleplanner.payload.request.employee.CreateEmployeeRequestDto;
import com.skapp.community.peopleplanner.payload.request.employee.EmployeeCommonDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.EmployeeEmergencyDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.EmployeeEmploymentDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.EmployeePersonalDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.EmployeeSystemPermissionsDto;
import com.skapp.community.peopleplanner.payload.request.employee.emergency.EmployeeEmergencyContactDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.employment.EmployeeEmploymentBasicDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.employment.EmployeeEmploymentBasicDetailsManagerDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.employment.EmployeeEmploymentIdentificationAndDiversityDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.employment.EmployeeEmploymentPreviousEmploymentDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeeExtraInfoDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeePersonalContactDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeePersonalEducationalDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeePersonalFamilyDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeePersonalGeneralDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeePersonalHealthAndOtherDetailsDto;
import com.skapp.community.peopleplanner.payload.request.employee.personal.EmployeePersonalSocialMediaDetailsDto;
import com.skapp.community.peopleplanner.payload.response.AnalyticsSearchResponseDto;
import com.skapp.community.peopleplanner.payload.response.CreateEmployeeResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeAllDataExportResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeBulkErrorResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeBulkResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeCountDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeCredentialsResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeDataExportResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeDataValidationResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeDetailedResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeManagerDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeManagerResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeePeriodResponseDto;
import com.skapp.community.peopleplanner.payload.response.EmployeeTeamDto;
import com.skapp.community.peopleplanner.payload.response.PrimarySecondaryOrTeamSupervisorResponseDto;
import com.skapp.community.peopleplanner.repository.EmployeeDao;
import com.skapp.community.peopleplanner.repository.EmployeeManagerDao;
import com.skapp.community.peopleplanner.repository.EmployeePeriodDao;
import com.skapp.community.peopleplanner.repository.EmployeeTeamDao;
import com.skapp.community.peopleplanner.repository.JobFamilyDao;
import com.skapp.community.peopleplanner.repository.JobTitleDao;
import com.skapp.community.peopleplanner.repository.TeamDao;
import com.skapp.community.peopleplanner.service.EmployeeValidationService;
import com.skapp.community.peopleplanner.service.PeopleEmailService;
import com.skapp.community.peopleplanner.service.PeopleService;
import com.skapp.community.peopleplanner.service.RolesService;
import com.skapp.community.peopleplanner.type.AccountStatus;
import com.skapp.community.peopleplanner.type.BulkItemStatus;
import com.skapp.community.peopleplanner.type.EmploymentType;
import com.skapp.community.peopleplanner.util.Validations;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.skapp.community.common.util.Validation.ADDRESS_REGEX;
import static com.skapp.community.common.util.Validation.ALPHANUMERIC_REGEX;
import static com.skapp.community.common.util.Validation.NAME_REGEX;
import static com.skapp.community.common.util.Validation.SPECIAL_CHAR_REGEX;
import static com.skapp.community.common.util.Validation.VALID_NIN_NUMBER_REGEXP;

@Service
@Slf4j
@RequiredArgsConstructor
public class PeopleServiceImpl implements PeopleService {

	private final UserService userService;

	private final MessageUtil messageUtil;

	private final PeopleMapper peopleMapper;

	private final UserDao userDao;

	private final TeamDao teamDao;

	private final EmployeeDao employeeDao;

	private final JobFamilyDao jobFamilyDao;

	private final JobTitleDao jobTitleDao;

	private final EmployeePeriodDao employeePeriodDao;

	private final EmployeeTeamDao employeeTeamDao;

	private final EmployeeManagerDao employeeManagerDao;

	private final PasswordEncoder passwordEncoder;

	private final RolesService rolesService;

	private final PageTransformer pageTransformer;

	private final PlatformTransactionManager transactionManager;

	private final PeopleEmailService peopleEmailService;

	private final ObjectMapper mapper;

	private final EncryptionDecryptionService encryptionDecryptionService;

	private final BulkContextService bulkContextService;

	private final AsyncEmailServiceImpl asyncEmailServiceImpl;

	private final ApplicationEventPublisher applicationEventPublisher;

	private final UserVersionService userVersionService;

	private final EmployeeValidationService employeeValidationService;

	@Value("${encryptDecryptAlgorithm.secret}")
	private String encryptSecret;

	@Override
	@Transactional
	public ResponseEntityDto createEmployee(CreateEmployeeRequestDto requestDto) {
		if (checkUserCountExceeded()) {
			throw new ModuleException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_LIMIT_EXCEEDED);
		}

		User user = new User();
		Employee employee = new Employee();

		employeeValidationService.validateCreateEmployeeRequestRequiredFields(requestDto, user);
		employeeValidationService.validateCreateEmployeeRequestPersonalDetails(requestDto.getPersonal(), user);
		employeeValidationService.validateCreateEmployeeRequestEmploymentDetails(requestDto.getEmployment(), user);
		rolesService.validateRoles(requestDto.getSystemPermissions(), user);

		employee.setUser(createUserEntity(user, requestDto));
		user.setEmployee(createEmployeeEntity(employee, requestDto));

		userDao.save(user);

		applicationEventPublisher.publishEvent(new UserCreatedEvent(this, user));
		peopleEmailService.sendUserInvitationEmail(user);
		addNewEmployeeTimeLineRecords(employee, requestDto);
		updateSubscriptionQuantity(1L, true);

		return new ResponseEntityDto(false, processCreateEmployeeResponse(user));
	}

	@Override
	public ResponseEntityDto quickAddEmployee(EmployeeQuickAddDto employeeQuickAddDto) {
		if (checkUserCountExceeded()) {
			throw new ModuleException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_LIMIT_EXCEEDED);
		}

		User user = new User();
		Employee employee = new Employee();

		CreateEmployeeRequestDto createEmployeeRequestDto = createEmployeeRequest(employeeQuickAddDto);
		employeeValidationService.validateCreateEmployeeRequestRequiredFields(createEmployeeRequestDto, user);
		rolesService.validateRoles(employeeQuickAddDto.getUserRoles(), user);

		user.setEmployee(createEmployeeEntity(employee, createEmployeeRequestDto));
		employee.setUser(createUserEntity(user, createEmployeeRequestDto));

		userDao.save(user);

		applicationEventPublisher.publishEvent(new UserCreatedEvent(this, user));
		peopleEmailService.sendUserInvitationEmail(user);
		addNewQuickUploadedEmployeeTimeLineRecords(employee, employeeQuickAddDto);
		updateSubscriptionQuantity(1L, true);

		return new ResponseEntityDto(false, processCreateEmployeeResponse(user));
	}

	@Override
	@Transactional
	public ResponseEntityDto updateEmployee(Long employeeId, CreateEmployeeRequestDto requestDto) {
		Optional<User> optionalUser = userDao.findById(employeeId);
		if (optionalUser.isEmpty()) {
			throw new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_NOT_FOUND);
		}

		User user = optionalUser.get();
		Employee employee = user.getEmployee();

		employeeValidationService.validateCreateEmployeeRequestRequiredFields(requestDto, user);
		employeeValidationService.validateCreateEmployeeRequestEmploymentDetails(requestDto.getEmployment(), user);
		employeeValidationService.validateCreateEmployeeRequestPersonalDetails(requestDto.getPersonal(), user);
		rolesService.validateRoles(requestDto.getSystemPermissions(), user);

		employee.setUser(createUserEntity(user, requestDto));
		user.setEmployee(createEmployeeEntity(employee, requestDto));

		userDao.save(user);

		return new ResponseEntityDto(false, requestDto);
	}

	private CreateEmployeeRequestDto createEmployeeRequest(EmployeeQuickAddDto dto) {
		CreateEmployeeRequestDto requestDto = new CreateEmployeeRequestDto();

		EmployeePersonalDetailsDto personalDetails = new EmployeePersonalDetailsDto();
		EmployeePersonalGeneralDetailsDto generalDetails = new EmployeePersonalGeneralDetailsDto();
		generalDetails.setFirstName(dto.getFirstName());
		generalDetails.setLastName(dto.getLastName());
		personalDetails.setGeneral(generalDetails);
		requestDto.setPersonal(personalDetails);

		EmployeeEmploymentDetailsDto employmentDetails = new EmployeeEmploymentDetailsDto();
		EmployeeEmploymentBasicDetailsDto basicDetails = new EmployeeEmploymentBasicDetailsDto();
		basicDetails.setEmail(dto.getEmail());
		employmentDetails.setEmploymentDetails(basicDetails);
		requestDto.setEmployment(employmentDetails);

		requestDto.setSystemPermissions(dto.getUserRoles());

		return requestDto;
	}

	private Employee createEmployeeEntity(Employee employee, CreateEmployeeRequestDto requestDto) {
		// Personal General Information
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getFirstName(),
				employee::setFirstName);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getMiddleName(),
				employee::setMiddleName);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getLastName(), employee::setLastName);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getGender(), employee::setGender);

		// Personal Contact Information
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getPersonalEmail(),
				employee::setPersonalEmail);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getContactNo(), employee::setPhone);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getAddressLine1(),
				employee::setAddressLine1);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getAddressLine2(),
				employee::setAddressLine2);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getCountry(), employee::setCountry);

		// Employment Details
		CommonModuleUtils.setIfExists(() -> requestDto.getEmployment().getEmploymentDetails().getEmployeeNumber(),
				employee::setIdentificationNo);
		CommonModuleUtils.setIfExists(() -> requestDto.getEmployment().getEmploymentDetails().getEmploymentAllocation(),
				employee::setEmploymentAllocation);
		CommonModuleUtils.setIfExists(() -> requestDto.getEmployment().getEmploymentDetails().getJoinedDate(),
				employee::setJoinDate);
		CommonModuleUtils.setIfExists(() -> requestDto.getEmployment().getEmploymentDetails().getWorkTimeZone(),
				employee::setTimeZone);

		// Identification and Diversity Details
		CommonModuleUtils.setIfExists(
				() -> requestDto.getEmployment().getIdentificationAndDiversityDetails().getEeoJobCategory(),
				employee::setEeo);

		// Common Information
		CommonModuleUtils.setIfExists(() -> requestDto.getCommon().getAuthPic(), employee::setAuthPic);

		CommonModuleUtils.setIfRequestValid(requestDto, () -> processEmployeePersonalInfo(requestDto, employee),
				employee::setPersonalInfo);

		CommonModuleUtils.setIfRequestValid(requestDto, () -> processEmergencyContacts(requestDto, employee),
				employee::setEmployeeEmergencies);

		CommonModuleUtils.setIfRequestValid(requestDto, () -> processEmployeeProbationPeriod(requestDto, employee),
				employee::setEmployeePeriods);

		CommonModuleUtils.setIfRequestValid(requestDto, () -> processCareerProgressions(requestDto, employee),
				employee::setEmployeeProgressions);

		CommonModuleUtils.setIfRequestValid(requestDto, () -> processEmployeeVisas(requestDto, employee),
				employee::setEmployeeVisas);

		CommonModuleUtils.setIfRequestValid(requestDto.getPersonal(),
				() -> processEmployeeFamilies(requestDto.getPersonal(), employee), employee::setEmployeeFamilies);

		CommonModuleUtils.setIfRequestValid(requestDto, () -> processEmployeeEducations(requestDto, employee),
				employee::setEmployeeEducations);

		CommonModuleUtils.setIfRequestValid(requestDto.getEmployment(),
				() -> processEmployeeTeams(requestDto.getEmployment(), employee), employee::setEmployeeTeams);

		CommonModuleUtils.setIfRequestValid(requestDto.getEmployment(),
				() -> processEmployeeManagers(requestDto.getEmployment(), employee), employee::setEmployeeManagers);

		CommonModuleUtils.setIfRequestValid(requestDto.getSystemPermissions(),
				() -> rolesService.assignRolesToEmployee(requestDto.getSystemPermissions(), employee),
				employee::setEmployeeRole);

		if (employee.getAccountStatus() == null) {
			employee.setAccountStatus(AccountStatus.PENDING);
		}

		return employee;
	}

	private User createUserEntity(User user, CreateEmployeeRequestDto requestDto) {
		if (user.getUserId() != null) {
			CommonModuleUtils.setIfExists(() -> createNotificationSettings(requestDto.getSystemPermissions(), user),
					user::setSettings);
			return user;
		}

		CommonModuleUtils.setIfExists(() -> requestDto.getEmployment().getEmploymentDetails().getEmail(),
				user::setEmail);

		LoginMethod loginMethod = userDao.findById(1L).map(User::getLoginMethod).orElse(LoginMethod.CREDENTIALS);

		if (loginMethod == LoginMethod.CREDENTIALS) {
			String tempPassword = CommonModuleUtils.generateSecureRandomPassword();
			CommonModuleUtils.setIfExists(() -> encryptionDecryptionService.encrypt(tempPassword, encryptSecret),
					user::setTempPassword);
			CommonModuleUtils.setIfExists(() -> passwordEncoder.encode(tempPassword), user::setPassword);
			user.setIsPasswordChangedForTheFirstTime(false);
		}
		else if (loginMethod == LoginMethod.GOOGLE) {
			user.setIsPasswordChangedForTheFirstTime(true);
		}

		CommonModuleUtils.setIfExists(() -> createNotificationSettings(requestDto.getSystemPermissions(), user),
				user::setSettings);
		user.setLoginMethod(loginMethod);
		user.setIsActive(true);

		return user;
	}

	private EmployeePersonalInfo processEmployeePersonalInfo(CreateEmployeeRequestDto requestDto, Employee employee) {
		EmployeePersonalInfo personalInfo = employee.getPersonalInfo();
		if (personalInfo == null) {
			personalInfo = new EmployeePersonalInfo();
		}

		// General information
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getDateOfBirth(),
				personalInfo::setBirthDate);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getNationality(),
				personalInfo::setNationality);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getNin(), personalInfo::setNin);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getPassportNumber(),
				personalInfo::setPassportNo);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getGeneral().getMaritalStatus(),
				personalInfo::setMaritalStatus);

		// Contact information
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getCity(), personalInfo::setCity);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getState(), personalInfo::setState);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getContact().getPostalCode(),
				personalInfo::setPostalCode);

		// Health information
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getHealthAndOther().getBloodGroup(),
				personalInfo::setBloodGroup);

		// Identification and diversity
		CommonModuleUtils.setIfExists(() -> requestDto.getEmployment().getIdentificationAndDiversityDetails().getSsn(),
				personalInfo::setSsn);
		CommonModuleUtils.setIfExists(
				() -> requestDto.getEmployment().getIdentificationAndDiversityDetails().getEthnicity(),
				personalInfo::setEthnicity);

		// Social media details
		EmployeePersonalSocialMediaDetailsDto socialMedia = CommonModuleUtils
			.safeGet(() -> requestDto.getPersonal().getSocialMedia());
		if (socialMedia == null) {
			socialMedia = new EmployeePersonalSocialMediaDetailsDto();
		}
		personalInfo.setSocialMediaDetails(mapper.valueToTree(socialMedia));

		// Extra info
		EmployeeExtraInfoDto extraInfo = new EmployeeExtraInfoDto();
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getHealthAndOther().getAllergies(),
				extraInfo::setAllergies);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getHealthAndOther().getTShirtSize(),
				extraInfo::setTShirtSize);
		CommonModuleUtils.setIfExists(() -> requestDto.getPersonal().getHealthAndOther().getDietaryRestrictions(),
				extraInfo::setDietaryRestrictions);
		personalInfo.setExtraInfo(mapper.valueToTree(extraInfo));

		// Previous employment
		CommonModuleUtils.setIfExists(() -> mapper.valueToTree(requestDto.getEmployment().getPreviousEmployment()),
				personalInfo::setPreviousEmploymentDetails);

		personalInfo.setEmployee(employee);
		return personalInfo;
	}

	private List<EmployeeFamily> processEmployeeFamilies(EmployeePersonalDetailsDto personal, Employee employee) {
		if (personal == null || employee == null || personal.getFamily() == null) {
			return new ArrayList<>();
		}

		List<EmployeeFamily> existingFamilies = employee.getEmployeeFamilies() != null
				? new ArrayList<>(employee.getEmployeeFamilies()) : new ArrayList<>();

		Map<Long, EmployeeFamily> existingFamilyMap = existingFamilies.stream()
			.filter(family -> family.getFamilyId() != null)
			.collect(Collectors.toMap(EmployeeFamily::getFamilyId, family -> family));

		List<EmployeeFamily> result = new ArrayList<>();

		for (EmployeePersonalFamilyDetailsDto dto : personal.getFamily()) {
			EmployeeFamily family = existingFamilyMap.containsKey(dto.getFamilyId())
					? existingFamilyMap.remove(dto.getFamilyId()) : new EmployeeFamily();

			family.setEmployee(employee);

			CommonModuleUtils.setIfNotNull(dto.getFirstName(), family::setFirstName);
			CommonModuleUtils.setIfNotNull(dto.getLastName(), family::setLastName);
			CommonModuleUtils.setIfNotNull(dto.getGender(), family::setGender);
			CommonModuleUtils.setIfNotNull(dto.getRelationship(), family::setFamilyRelationship);
			CommonModuleUtils.setIfNotNull(dto.getDateOfBirth(), family::setBirthDate);
			CommonModuleUtils.setIfNotNull(dto.getParentName(), family::setParentName);

			result.add(family);
		}

		result.addAll(existingFamilyMap.values());
		return result;
	}

	private List<EmployeeEducation> processEmployeeEducations(CreateEmployeeRequestDto requestDto, Employee employee) {
		List<EmployeePersonalEducationalDetailsDto> educationalDtos = CommonModuleUtils
			.safeGet(() -> requestDto.getPersonal().getEducational());

		if (educationalDtos == null || employee == null) {
			return new ArrayList<>();
		}

		List<EmployeeEducation> existingEducations = employee.getEmployeeEducations() != null
				? new ArrayList<>(employee.getEmployeeEducations()) : new ArrayList<>();

		Map<Long, EmployeeEducation> existingEducationMap = existingEducations.stream()
			.filter(education -> education.getEducationId() != null)
			.collect(Collectors.toMap(EmployeeEducation::getEducationId, education -> education));

		List<EmployeeEducation> result = new ArrayList<>();

		for (EmployeePersonalEducationalDetailsDto dto : educationalDtos) {
			EmployeeEducation education = existingEducationMap.containsKey(dto.getEducationId())
					? existingEducationMap.remove(dto.getEducationId()) : new EmployeeEducation();
			education.setEmployee(employee);

			CommonModuleUtils.setIfExists(dto::getInstitutionName, education::setInstitution);
			CommonModuleUtils.setIfExists(dto::getDegree, education::setDegree);
			CommonModuleUtils.setIfExists(dto::getMajor, education::setSpecialization);
			CommonModuleUtils.setIfExists(dto::getStartDate, education::setStartDate);
			CommonModuleUtils.setIfExists(dto::getEndDate, education::setEndDate);

			result.add(education);
		}

		result.addAll(existingEducationMap.values());
		return result;
	}

	private Set<EmployeeTeam> processEmployeeTeams(EmployeeEmploymentDetailsDto requestDto, Employee employee) {
		if (requestDto == null || requestDto.getEmploymentDetails() == null || employee == null) {
			return new HashSet<>();
		}

		Long[] teamIds = requestDto.getEmploymentDetails().getTeamIds();
		if (teamIds == null || teamIds.length == 0) {
			return new HashSet<>();
		}

		Set<EmployeeTeam> existingTeams = employee.getEmployeeTeams() != null ? employee.getEmployeeTeams()
				: new HashSet<>();

		Set<Long> existingTeamIds = existingTeams.stream()
			.map(employeeTeam -> employeeTeam.getTeam() != null ? employeeTeam.getTeam().getTeamId() : null)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		Set<EmployeeTeam> result = Arrays.stream(teamIds).map(teamId -> {
			Team team = teamDao.findByTeamId(teamId);
			EmployeeTeam employeeTeam = existingTeamIds.contains(teamId) ? existingTeams.stream()
				.filter(et -> et.getTeam() != null && et.getTeam().getTeamId().equals(teamId))
				.findFirst()
				.orElse(new EmployeeTeam()) : new EmployeeTeam();

			CommonModuleUtils.setIfExists(() -> team, employeeTeam::setTeam);
			CommonModuleUtils.setIfExists(() -> false, employeeTeam::setIsSupervisor);
			CommonModuleUtils.setIfExists(() -> employee, employeeTeam::setEmployee);

			return employeeTeam;
		}).collect(Collectors.toSet());

		Set<Long> requestTeamIds = Arrays.stream(teamIds).collect(Collectors.toSet());

		existingTeams.stream()
			.filter(et -> et.getTeam() != null && !requestTeamIds.contains(et.getTeam().getTeamId()))
			.forEach(result::add);

		return result;
	}

	private Set<EmployeeManager> processEmployeeManagers(EmployeeEmploymentDetailsDto requestDto, Employee employee) {
		if (requestDto == null || requestDto.getEmploymentDetails() == null || employee == null) {
			return new HashSet<>();
		}

		EmployeeEmploymentBasicDetailsManagerDetailsDto primarySupervisor = requestDto.getEmploymentDetails()
			.getPrimarySupervisor();
		EmployeeEmploymentBasicDetailsManagerDetailsDto secondarySupervisor = requestDto.getEmploymentDetails()
			.getSecondarySupervisor();

		Set<EmployeeManager> existingManagers = employee.getEmployeeManagers() != null
				? new HashSet<>(employee.getEmployeeManagers()) : new HashSet<>();
		Set<EmployeeManager> result = new HashSet<>();

		if (primarySupervisor != null && primarySupervisor.getEmployeeId() != null) {
			Employee manager = employeeDao.findEmployeeByEmployeeId(primarySupervisor.getEmployeeId());
			if (manager != null) {
				EmployeeManager primary = existingManagers.stream()
					.filter(em -> em.getManager() != null
							&& em.getManager().getEmployeeId().equals(primarySupervisor.getEmployeeId())
							&& em.getManagerType() == ManagerType.PRIMARY)
					.findFirst()
					.orElse(new EmployeeManager());

				CommonModuleUtils.setIfExists(() -> manager, primary::setManager);
				CommonModuleUtils.setIfExists(() -> employee, primary::setEmployee);
				CommonModuleUtils.setIfExists(() -> ManagerType.PRIMARY, primary::setManagerType);
				CommonModuleUtils.setIfExists(() -> true, primary::setIsPrimaryManager);
				result.add(primary);
			}
		}

		if (secondarySupervisor != null && secondarySupervisor.getEmployeeId() != null && (primarySupervisor == null
				|| !secondarySupervisor.getEmployeeId().equals(primarySupervisor.getEmployeeId()))) {
			Employee manager = employeeDao.findEmployeeByEmployeeId(secondarySupervisor.getEmployeeId());
			if (manager != null) {
				EmployeeManager secondary = existingManagers.stream()
					.filter(em -> em.getManager() != null
							&& em.getManager().getEmployeeId().equals(secondarySupervisor.getEmployeeId())
							&& em.getManagerType() == ManagerType.SECONDARY)
					.findFirst()
					.orElse(new EmployeeManager());
				CommonModuleUtils.setIfExists(() -> manager, secondary::setManager);
				CommonModuleUtils.setIfExists(() -> employee, secondary::setEmployee);
				CommonModuleUtils.setIfExists(() -> ManagerType.SECONDARY, secondary::setManagerType);
				CommonModuleUtils.setIfExists(() -> false, secondary::setIsPrimaryManager);
				result.add(secondary);
			}
		}

		existingManagers.stream().filter(em -> !result.contains(em)).forEach(result::add);

		return result;
	}

	private List<EmployeeEmergency> processEmergencyContacts(CreateEmployeeRequestDto requestDto, Employee employee) {
		if (requestDto == null || requestDto.getEmergency() == null || employee == null) {
			return new ArrayList<>();
		}

		List<EmployeeEmergency> existingContacts = employee.getEmployeeEmergencies() != null
				? employee.getEmployeeEmergencies() : new ArrayList<>();

		List<EmployeeEmergency> result = new ArrayList<>();

		if (requestDto.getEmergency().getPrimaryEmergencyContact() != null) {
			result.add(processSingleEmergencyContact(requestDto.getEmergency().getPrimaryEmergencyContact(), true,
					employee, existingContacts));
		}

		if (requestDto.getEmergency().getSecondaryEmergencyContact() != null) {
			result.add(processSingleEmergencyContact(requestDto.getEmergency().getSecondaryEmergencyContact(), false,
					employee, existingContacts));
		}

		existingContacts.stream().filter(existing -> !result.contains(existing)).forEach(result::add);

		return result;
	}

	private EmployeeEmergency processSingleEmergencyContact(EmployeeEmergencyContactDetailsDto emergencyDto,
			boolean isPrimary, Employee employee, List<EmployeeEmergency> existingContacts) {
		EmployeeEmergency existingContact = existingContacts.stream()
			.filter(contact -> contact.getIsPrimary() == isPrimary)
			.findFirst()
			.orElse(new EmployeeEmergency());

		existingContact.setEmployee(employee);
		existingContact.setIsPrimary(isPrimary);
		CommonModuleUtils.setIfExists(emergencyDto::getName, existingContact::setName);
		CommonModuleUtils.setIfExists(emergencyDto::getRelationship, existingContact::setEmergencyRelationship);
		if (emergencyDto.getContactNo() != null && emergencyDto.getCountryCode() != null) {
			existingContact.setContactNo(emergencyDto.getCountryCode() + emergencyDto.getContactNo());
		}

		return existingContact;
	}

	private List<EmployeeVisa> processEmployeeVisas(CreateEmployeeRequestDto requestDto, Employee employee) {
		if (requestDto == null || requestDto.getEmployment() == null
				|| requestDto.getEmployment().getVisaDetails() == null || employee == null) {
			return new ArrayList<>();
		}

		List<EmployeeVisa> existingVisas = employee.getEmployeeVisas() != null ? employee.getEmployeeVisas()
				: new ArrayList<>();

		Map<Long, EmployeeVisa> existingVisaMap = existingVisas.stream()
			.filter(visa -> visa.getVisaId() != null)
			.collect(Collectors.toMap(EmployeeVisa::getVisaId, visa -> visa));

		List<EmployeeVisa> result = requestDto.getEmployment().getVisaDetails().stream().map(dto -> {
			EmployeeVisa visa = existingVisaMap.containsKey(dto.getVisaId()) ? existingVisaMap.remove(dto.getVisaId())
					: new EmployeeVisa();
			visa.setEmployee(employee);

			CommonModuleUtils.setIfExists(dto::getVisaType, visa::setVisaType);
			CommonModuleUtils.setIfExists(dto::getIssuingCountry, visa::setIssuingCountry);
			CommonModuleUtils.setIfExists(dto::getIssuedDate, visa::setIssuedDate);
			CommonModuleUtils.setIfExists(dto::getExpiryDate, visa::setExpirationDate);

			return visa;
		}).collect(Collectors.toList());

		result.addAll(existingVisaMap.values());
		return result;
	}

	private Set<EmployeePeriod> processEmployeeProbationPeriod(CreateEmployeeRequestDto requestDto, Employee employee) {
		Set<EmployeePeriod> existingPeriods = employee.getEmployeePeriods() != null ? employee.getEmployeePeriods()
				: new HashSet<>();

		Set<EmployeePeriod> result = new HashSet<>();

		if (requestDto == null || requestDto.getEmployment() == null
				|| requestDto.getEmployment().getEmploymentDetails() == null) {
			return existingPeriods.stream().filter(EmployeePeriod::getIsActive).collect(Collectors.toSet());
		}

		EmployeeEmploymentBasicDetailsDto employmentDetails = requestDto.getEmployment().getEmploymentDetails();

		EmployeePeriod period = existingPeriods.stream()
			.filter(p -> p.getStartDate() != null && p.getEndDate() != null)
			.findFirst()
			.orElse(new EmployeePeriod());

		CommonModuleUtils.setIfExists(employmentDetails::getProbationStartDate, period::setStartDate);
		CommonModuleUtils.setIfExists(employmentDetails::getProbationEndDate, period::setEndDate);
		CommonModuleUtils.setIfExists(() -> true, period::setIsActive);
		CommonModuleUtils.setIfExists(() -> employee, period::setEmployee);

		result.add(period);
		existingPeriods.stream().filter(existing -> !result.contains(existing)).forEach(result::add);

		return result;
	}

	private List<EmployeeProgression> processCareerProgressions(CreateEmployeeRequestDto requestDto,
			Employee employee) {
		if (requestDto == null || requestDto.getEmployment() == null
				|| requestDto.getEmployment().getCareerProgression() == null || employee == null) {
			return new ArrayList<>();
		}

		List<EmployeeProgression> existingProgressions = employee.getEmployeeProgressions() != null
				? employee.getEmployeeProgressions() : new ArrayList<>();

		Map<Long, EmployeeProgression> existingProgressionMap = existingProgressions.stream()
			.filter(progression -> progression.getProgressionId() != null)
			.collect(Collectors.toMap(EmployeeProgression::getProgressionId, progression -> progression));

		List<EmployeeProgression> result = requestDto.getEmployment().getCareerProgression().stream().map(dto -> {
			EmployeeProgression progression = existingProgressionMap.containsKey(dto.getProgressionId())
					? existingProgressionMap.remove(dto.getProgressionId()) : new EmployeeProgression();
			progression.setEmployee(employee);

			CommonModuleUtils.setIfExists(dto::getEmploymentType, progression::setEmploymentType);
			CommonModuleUtils.setIfExists(dto::getJobFamilyId, progression::setJobFamilyId);
			CommonModuleUtils.setIfExists(dto::getJobTitleId, progression::setJobTitleId);
			CommonModuleUtils.setIfExists(dto::getStartDate, progression::setStartDate);
			CommonModuleUtils.setIfExists(dto::getEndDate, progression::setEndDate);
			CommonModuleUtils.setIfExists(dto::getIsCurrentEmployment, progression::setIsCurrent);

			if (Boolean.TRUE.equals(dto.getIsCurrentEmployment())) {
				CommonModuleUtils.setIfExists(dto::getEmploymentType, employee::setEmploymentType);
				CommonModuleUtils.setIfExists(() -> jobFamilyDao.getJobFamilyById(dto.getJobFamilyId()),
						employee::setJobFamily);
				CommonModuleUtils.setIfExists(() -> jobTitleDao.getJobTitleById(dto.getJobTitleId()),
						employee::setJobTitle);
			}

			return progression;
		}).collect(Collectors.toList());

		result.addAll(existingProgressionMap.values());
		return result;
	}

	private ResponseEntityDto processCreateEmployeeResponse(User user) {
		Employee employee = user.getEmployee();
		CreateEmployeeResponseDto responseDto = peopleMapper.employeeToCreateEmployeeResponseDto(employee);

		if (user.getLoginMethod() == LoginMethod.CREDENTIALS) {
			EmployeeCredentialsResponseDto credentials = new EmployeeCredentialsResponseDto();
			credentials.setEmail(employee.getUser() != null ? employee.getUser().getEmail() : null);
			credentials.setTempPassword(encryptionDecryptionService.decrypt(user.getTempPassword(), encryptSecret));
			responseDto.setEmployeeCredentials(credentials);
		}

		return new ResponseEntityDto(false, responseDto);
	}

	@Override
	@Transactional
	public ResponseEntityDto getEmployees(EmployeeFilterDto employeeFilterDto) {
		log.info("getEmployees: execution started");
		int pageSize = employeeFilterDto.getSize();

		boolean isExport = employeeFilterDto.getIsExport();
		if (isExport) {
			pageSize = (int) employeeDao.count();
		}

		Pageable pageable = PageRequest.of(employeeFilterDto.getPage(), pageSize,
				Sort.by(employeeFilterDto.getSortOrder(), employeeFilterDto.getSortKey().toString()));

		Page<Employee> employees = employeeDao.findEmployees(employeeFilterDto, pageable);
		PageDto pageDto = pageTransformer.transform(employees);

		List<Long> employeeIds = employees.stream().map(Employee::getEmployeeId).toList();
		List<EmployeeTeamDto> teamList = employeeDao.findTeamsByEmployees(employeeIds);

		if (!isExport) {
			pageDto.setItems(fetchEmployeeSearchData(employees));

			log.info("getEmployees: Successfully executed");
			return new ResponseEntityDto(false, pageDto);
		}
		else {
			List<EmployeeDataExportResponseDto> responseDtos = exportEmployeeData(employees, teamList, employeeIds);
			log.info("getEmployees: Successfully finished returning {} employees on exportEmployeeData",
					responseDtos.size());
			return new ResponseEntityDto(false, responseDtos);
		}
	}

	@Override
	public ResponseEntityDto exportEmployees(EmployeeExportFilterDto employeeExportFilterDto) {
		User currentUser = userService.getCurrentUser();
		log.info("exportEmployees: execution started by user: {}", currentUser.getUserId());

		List<Employee> employees = employeeDao.findEmployeesForExport(employeeExportFilterDto);

		List<Long> employeeIds = employees.stream().map(Employee::getEmployeeId).toList();
		List<EmployeeTeamDto> teamList = employeeDao.findTeamsByEmployees(employeeIds);

		List<EmployeeAllDataExportResponseDto> responseDtos = exportAllEmployeeData(employees, teamList, employeeIds);
		log.info("exportEmployees: Successfully finished returning {} employees on exportEmployeeData",
				responseDtos.size());

		return new ResponseEntityDto(false, responseDtos);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntityDto getEmployeeById(Long employeeId) {
		return new ResponseEntityDto(false, mapEmployeeToDto(employeeDao.findById(employeeId)
			.orElseThrow(() -> new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_NOT_FOUND))));
	}

	private CreateEmployeeRequestDto mapEmployeeToDto(Employee employee) {
		CreateEmployeeRequestDto dto = new CreateEmployeeRequestDto();
		dto.setPersonal(mapPersonalDetails(employee));
		dto.setEmergency(mapEmergencyDetails(employee));
		dto.setEmployment(mapEmploymentDetails(employee));
		dto.setSystemPermissions(mapSystemPermissions(employee));
		dto.setCommon(mapCommonDetails(employee));
		return dto;
	}

	private EmployeePersonalDetailsDto mapPersonalDetails(Employee employee) {
		EmployeePersonalDetailsDto dto = new EmployeePersonalDetailsDto();
		dto.setGeneral(mapPersonalGeneralDetails(employee));
		dto.setContact(mapPersonalContactDetails(employee));

		Optional.ofNullable(employee.getEmployeeFamilies())
			.ifPresent(families -> dto
				.setFamily(families.stream().map(peopleMapper::employeeFamilyToFamilyDetailsDto).toList()));

		Optional.ofNullable(employee.getEmployeeEducations())
			.ifPresent(educations -> dto.setEducational(
					educations.stream().map(peopleMapper::employeeEducationToEducationalDetailsDto).toList()));

		dto.setSocialMedia(mapPersonalSocialMediaDetails(employee));
		dto.setHealthAndOther(mapPersonalHealthAndOtherDetails(employee));
		return dto;
	}

	private EmployeePersonalGeneralDetailsDto mapPersonalGeneralDetails(Employee employee) {
		EmployeePersonalGeneralDetailsDto dto = new EmployeePersonalGeneralDetailsDto();
		dto.setFirstName(employee.getFirstName());
		dto.setMiddleName(employee.getMiddleName());
		dto.setLastName(employee.getLastName());
		dto.setGender(employee.getGender());
		dto.setNin(employee.getIdentificationNo());

		Optional.ofNullable(employee.getPersonalInfo()).ifPresent(personalInfo -> {
			dto.setDateOfBirth(personalInfo.getBirthDate());
			dto.setNationality(personalInfo.getNationality());
			dto.setPassportNumber(personalInfo.getPassportNo());
			dto.setMaritalStatus(personalInfo.getMaritalStatus());
		});

		return dto;
	}

	private EmployeePersonalContactDetailsDto mapPersonalContactDetails(Employee employee) {
		EmployeePersonalContactDetailsDto dto = new EmployeePersonalContactDetailsDto();
		dto.setPersonalEmail(employee.getPersonalEmail());
		dto.setContactNo(employee.getPhone());
		dto.setAddressLine1(employee.getAddressLine1());
		dto.setAddressLine2(employee.getAddressLine2());
		dto.setCountry(employee.getCountry());

		Optional.ofNullable(employee.getPersonalInfo()).ifPresent(personalInfo -> {
			dto.setCity(personalInfo.getCity());
			dto.setState(personalInfo.getState());
			dto.setPostalCode(personalInfo.getPostalCode());
		});

		return dto;
	}

	private EmployeePersonalSocialMediaDetailsDto mapPersonalSocialMediaDetails(Employee employee) {
		if (employee.getPersonalInfo() != null && employee.getPersonalInfo().getSocialMediaDetails() != null) {
			try {
				return mapper.treeToValue(employee.getPersonalInfo().getSocialMediaDetails(),
						EmployeePersonalSocialMediaDetailsDto.class);
			}
			catch (JsonProcessingException e) {
				log.error("Error converting social media details JSON to DTO", e);
			}
		}
		return new EmployeePersonalSocialMediaDetailsDto();
	}

	private EmployeePersonalHealthAndOtherDetailsDto mapPersonalHealthAndOtherDetails(Employee employee) {
		EmployeePersonalHealthAndOtherDetailsDto dto = new EmployeePersonalHealthAndOtherDetailsDto();

		if (employee.getPersonalInfo() != null) {
			EmployeePersonalInfo personalInfo = employee.getPersonalInfo();
			dto.setBloodGroup(personalInfo.getBloodGroup());

			if (personalInfo.getExtraInfo() != null) {
				try {
					EmployeeExtraInfoDto extraInfo = mapper.treeToValue(personalInfo.getExtraInfo(),
							EmployeeExtraInfoDto.class);
					dto.setAllergies(extraInfo.getAllergies());
					dto.setDietaryRestrictions(extraInfo.getDietaryRestrictions());
					dto.setTShirtSize(extraInfo.getTShirtSize());
				}
				catch (JsonProcessingException e) {
					log.error("Error converting extra info JSON to DTO", e);
				}
			}
		}

		return dto;
	}

	private EmployeeEmergencyDetailsDto mapEmergencyDetails(Employee employee) {
		EmployeeEmergencyDetailsDto dto = new EmployeeEmergencyDetailsDto();

		if (employee.getEmployeeEmergencies() != null && !employee.getEmployeeEmergencies().isEmpty()) {
			List<EmployeeEmergency> emergencies = new ArrayList<>(employee.getEmployeeEmergencies());

			emergencies.stream()
				.filter(EmployeeEmergency::getIsPrimary)
				.findFirst()
				.or(() -> emergencies.isEmpty() ? Optional.empty() : Optional.of(emergencies.getFirst()))
				.ifPresent(e -> dto.setPrimaryEmergencyContact(peopleMapper.employeeEmergencyToEmergencyContactDto(e)));

			emergencies.stream()
				.filter(e -> !e.getIsPrimary())
				.findFirst()
				.ifPresent(
						e -> dto.setSecondaryEmergencyContact(peopleMapper.employeeEmergencyToEmergencyContactDto(e)));
		}

		return dto;
	}

	private EmployeeEmploymentDetailsDto mapEmploymentDetails(Employee employee) {
		EmployeeEmploymentDetailsDto dto = new EmployeeEmploymentDetailsDto();
		dto.setEmploymentDetails(mapEmploymentBasicDetails(employee));

		Optional.ofNullable(employee.getEmployeeProgressions())
			.ifPresent(progressions -> dto.setCareerProgression(progressions.stream()
				.map(peopleMapper::employeeProgressionToCareerProgressionDto)
				.collect(Collectors.toList())));

		dto.setIdentificationAndDiversityDetails(mapIdentificationAndDiversityDetails(employee));
		dto.setPreviousEmployment(mapPreviousEmploymentDetails(employee));

		Optional.ofNullable(employee.getEmployeeVisas())
			.ifPresent(visas -> dto
				.setVisaDetails(visas.stream().map(peopleMapper::employeeVisaToVisaDetailsDto).toList()));

		return dto;
	}

	private EmployeeEmploymentIdentificationAndDiversityDetailsDto mapIdentificationAndDiversityDetails(
			Employee employee) {
		EmployeeEmploymentIdentificationAndDiversityDetailsDto dto = new EmployeeEmploymentIdentificationAndDiversityDetailsDto();

		Optional.ofNullable(employee.getPersonalInfo()).ifPresent(personalInfo -> {
			dto.setSsn(personalInfo.getSsn());
			dto.setEthnicity(personalInfo.getEthnicity());
		});

		dto.setEeoJobCategory(employee.getEeo());

		return dto;
	}

	private List<EmployeeEmploymentPreviousEmploymentDetailsDto> mapPreviousEmploymentDetails(Employee employee) {
		if (employee.getPersonalInfo() != null && employee.getPersonalInfo().getPreviousEmploymentDetails() != null) {
			try {
				return mapper.treeToValue(employee.getPersonalInfo().getPreviousEmploymentDetails(),
						new TypeReference<>() {
						});
			}
			catch (JsonProcessingException e) {
				log.error("Error converting previous employment details JSON to DTO", e);
			}
		}
		return null;
	}

	private EmployeeEmploymentBasicDetailsDto mapEmploymentBasicDetails(Employee employee) {
		EmployeeEmploymentBasicDetailsDto dto = new EmployeeEmploymentBasicDetailsDto();
		dto.setJoinedDate(employee.getJoinDate());
		dto.setWorkTimeZone(employee.getTimeZone());
		dto.setEmploymentAllocation(employee.getEmploymentAllocation());

		Optional.ofNullable(employee.getUser()).ifPresent(user -> {
			dto.setEmployeeNumber(employee.getIdentificationNo());
			dto.setEmail(user.getEmail());
		});

		Optional.ofNullable(employee.getEmployeeTeams())
			.ifPresent(teams -> dto
				.setTeamIds(teams.stream().map(team -> team.getTeam().getTeamId()).toArray(Long[]::new)));

		if (employee.getEmployeeManagers() != null) {
			dto.setPrimarySupervisor(employee.getEmployeeManagers()
				.stream()
				.filter(EmployeeManager::getIsPrimaryManager)
				.findFirst()
				.map(peopleMapper::employeeManagerToManagerDetailsDto)
				.orElse(null));

			dto.setSecondarySupervisor(employee.getEmployeeManagers()
				.stream()
				.filter(m -> !m.getIsPrimaryManager())
				.findFirst()
				.map(peopleMapper::employeeManagerToManagerDetailsDto)
				.orElse(null));
		}

		Optional.ofNullable(employee.getEmployeePeriods())
			.flatMap(periods -> periods.stream().findFirst())
			.ifPresent(probation -> {
				dto.setProbationStartDate(probation.getStartDate());
				dto.setProbationEndDate(probation.getEndDate());
			});

		return dto;
	}

	private EmployeeSystemPermissionsDto mapSystemPermissions(Employee employee) {
		EmployeeSystemPermissionsDto dto = new EmployeeSystemPermissionsDto();

		Optional.ofNullable(employee.getEmployeeRole()).ifPresent(role -> {
			dto.setIsSuperAdmin(role.getIsSuperAdmin());
			dto.setPeopleRole(role.getPeopleRole());
			dto.setLeaveRole(role.getLeaveRole());
			dto.setAttendanceRole(role.getAttendanceRole());
			dto.setEsignRole(role.getEsignRole());
		});

		return dto;
	}

	private EmployeeCommonDetailsDto mapCommonDetails(Employee employee) {
		EmployeeCommonDetailsDto dto = new EmployeeCommonDetailsDto();
		dto.setAccountStatus(employee.getAccountStatus());
		dto.setAuthPic(employee.getAuthPic());
		dto.setEmployeeId(employee.getEmployeeId());
		dto.setJobTitle(employee.getJobTitle() != null ? employee.getJobTitle().getName() : null);
		return dto;
	}

	@Override
	@Transactional
	public ResponseEntityDto getCurrentEmployee() {
		User user = userService.getCurrentUser();
		Optional<Employee> employee = employeeDao.findById(user.getUserId());
		if (employee.isEmpty()) {
			throw new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_NOT_FOUND);
		}

		EmployeeDetailedResponseDto employeeDetailedResponseDto = peopleMapper
			.employeeToEmployeeDetailedResponseDto(employee.get());
		Optional<EmployeePeriod> period = employeePeriodDao
			.findEmployeePeriodByEmployee_EmployeeId(employee.get().getEmployeeId());

		if (employee.get().getEmployeeRole() != null) {
			employeeDetailedResponseDto
				.setEmployeeRole(peopleMapper.employeeRoleToEmployeeRoleResponseDto(employee.get().getEmployeeRole()));
		}

		if (period.isPresent()) {
			EmployeePeriodResponseDto periodResponseDto = peopleMapper
				.employeePeriodToEmployeePeriodResponseDto(period.get());
			employeeDetailedResponseDto.setPeriodResponseDto(periodResponseDto);
		}

		return new ResponseEntityDto(false, employeeDetailedResponseDto);
	}

	@Override
	@Transactional
	public ResponseEntityDto addBulkEmployees(List<EmployeeBulkDto> employeeBulkDtoList) {
		List<EmployeeBulkDto> validEmployeeBulkDtoList = getValidEmployeeBulkDtoList(employeeBulkDtoList);
		User currentUser = userService.getCurrentUser();
		log.info("addEmployeeBulk: execution started by user: {}", currentUser.getUserId());

		ExecutorService executorService = Executors.newFixedThreadPool(6);
		List<EmployeeBulkResponseDto> results = Collections.synchronizedList(new ArrayList<>());
		AtomicReference<ResponseEntityDto> outValues = new AtomicReference<>(new ResponseEntityDto());

		List<CompletableFuture<Void>> tasks = createEmployeeTasks(validEmployeeBulkDtoList, executorService, results);
		waitForTaskCompletion(tasks, executorService);

		asyncEmailServiceImpl.sendEmailsInBackground(results);

		generateBulkErrorResponse(outValues, employeeBulkDtoList.size(), results);
		List<EmployeeBulkDto> overflowedEmployeeBulkDtoList = getOverFlowedEmployeeBulkDtoList(employeeBulkDtoList,
				validEmployeeBulkDtoList);

		List<EmployeeBulkResponseDto> totalResults = getTotalResultList(results, overflowedEmployeeBulkDtoList);

		int successCount = generateBulkErrorResponse(outValues, employeeBulkDtoList.size(), totalResults);
		updateSubscriptionQuantity(successCount, true);

		addNewBulkUploadedEmployeeTimeLineRecords(totalResults);

		return outValues.get();
	}

	@Override
	@Transactional
	public ResponseEntityDto getLoginPendingEmployeeCount() {
		User currentUser = userService.getCurrentUser();
		log.info("getLoginPendingEmployeeCount: execution started by user: {}", currentUser.getUserId());

		EmployeeCountDto employeeCount = employeeDao.getLoginPendingEmployeeCount();
		if (employeeCount == null) {
			throw new ModuleException(PeopleMessageConstant.PEOPLE_ERROR_LOGIN_PENDING_EMPLOYEES_NOT_FOUND);
		}

		return new ResponseEntityDto(false, employeeCount);
	}

	@Override
	@Transactional
	public ResponseEntityDto searchEmployeesByNameOrEmail(PermissionFilterDto permissionFilterDto) {
		log.info("searchEmployeesByNameOrEmail: execution started");

		List<Employee> employees = employeeDao.findEmployeeByNameEmail(permissionFilterDto.getKeyword(),
				permissionFilterDto);
		List<EmployeeDetailedResponseDto> employeeResponseDtos = peopleMapper
			.employeeListToEmployeeDetailedResponseDtoList(employees);

		log.info("searchEmployeesByNameOrEmail: execution ended");
		return new ResponseEntityDto(false, employeeResponseDtos);
	}

	@Override
	@Transactional
	public ResponseEntityDto searchEmployeesByEmail(String email) {
		log.info("searchEmployeesByEmail: execution started");

		Validations.validateEmail(email);
		Boolean isValidEmail = (employeeDao.findEmployeeByEmail(email) != null);

		log.info("searchEmployeesByEmail: execution ended");
		return new ResponseEntityDto(false, isValidEmail);
	}

	@Override
	@Transactional
	public ResponseEntityDto getEmployeeByIdOrEmail(EmployeeDataValidationDto employeeDataValidationDto) {
		log.info("getEmployeeByIdOrEmail: execution started");

		String workEmailCheck = employeeDataValidationDto.getWorkEmail();
		String identificationNoCheck = employeeDataValidationDto.getIdentificationNo();
		Optional<User> newUser = userDao.findByEmail(workEmailCheck);
		List<Employee> newEmployees = employeeDao.findByIdentificationNo(identificationNoCheck);
		EmployeeDataValidationResponseDto employeeDataValidationResponseDto = new EmployeeDataValidationResponseDto();
		employeeDataValidationResponseDto.setIsWorkEmailExists(newUser.isPresent());
		String userDomain = workEmailCheck.substring(workEmailCheck.indexOf("@") + 1);
		employeeDataValidationResponseDto.setIsGoogleDomain(Validation.ssoTypeMatches(userDomain));

		if (!newEmployees.isEmpty()) {
			employeeDataValidationResponseDto.setIsIdentificationNoExists(true);
		}

		log.info("getEmployeeByIdOrEmail: execution ended");
		return new ResponseEntityDto(false, employeeDataValidationResponseDto);
	}

	@Override
	@Transactional
	public ResponseEntityDto terminateUser(Long userId) {
		log.info("terminateUser: execution started");

		updateUserStatus(userId, AccountStatus.TERMINATED, false);

		log.info("terminateUser: execution ended");
		return new ResponseEntityDto(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_SUCCESS_EMPLOYEE_TERMINATED),
				false);
	}

	@Override
	@Transactional
	public ResponseEntityDto deleteUser(Long userId) {
		log.info("deleteUser: execution started");

		updateUserStatus(userId, AccountStatus.DELETED, true);
		log.info("deleteUser: execution ended");

		return new ResponseEntityDto(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_SUCCESS_EMPLOYEE_DELETED),
				false);
	}

	@Override
	@Transactional
	public List<EmployeeManagerResponseDto> getCurrentEmployeeManagers() {
		User user = userService.getCurrentUser();

		List<EmployeeManager> employeeManagers = employeeManagerDao.findByEmployee(user.getEmployee());
		return employeeManagers.stream().map(employeeManager -> {
			EmployeeManagerResponseDto responseDto = new EmployeeManagerResponseDto();
			Employee manager = employeeManager.getManager();

			responseDto.setEmployeeId(manager.getEmployeeId());
			responseDto.setFirstName(manager.getFirstName());
			responseDto.setLastName(manager.getLastName());
			responseDto.setMiddleName(manager.getMiddleName());
			responseDto.setAuthPic(manager.getAuthPic());
			responseDto.setIsPrimaryManager(employeeManager.getIsPrimaryManager());
			responseDto.setManagerType(employeeManager.getManagerType());

			return responseDto;
		}).toList();
	}

	@Override
	public ResponseEntityDto updateNotificationSettings(
			NotificationSettingsPatchRequestDto notificationSettingsPatchRequestDto) {
		log.info("updateNotificationSettings: execution started");

		User currentUser = userService.getCurrentUser();
		Optional<User> optionalUser = userDao.findById(currentUser.getUserId());
		if (optionalUser.isEmpty()) {
			throw new ModuleException(CommonMessageConstant.COMMON_ERROR_USER_NOT_FOUND);
		}
		User user = optionalUser.get();

		UserSettings userSettings;
		if (user.getSettings() != null) {
			userSettings = user.getSettings();
		}
		else {
			userSettings = new UserSettings();
			userSettings.setUser(user);
			user.setSettings(userSettings);
		}

		ObjectNode notificationsObjectNode = mapper.createObjectNode();

		notificationsObjectNode.put(NotificationSettingsType.LEAVE_REQUEST.getKey(),
				notificationSettingsPatchRequestDto.getIsLeaveRequestNotificationsEnabled());
		notificationsObjectNode.put(NotificationSettingsType.TIME_ENTRY.getKey(),
				notificationSettingsPatchRequestDto.getIsTimeEntryNotificationsEnabled());
		notificationsObjectNode.put(NotificationSettingsType.LEAVE_REQUEST_NUDGE.getKey(),
				notificationSettingsPatchRequestDto.getIsLeaveRequestNudgeNotificationsEnabled());

		userSettings.setNotifications(notificationsObjectNode);
		user.setSettings(userSettings);

		userDao.save(user);

		log.info("updateNotificationSettings: execution ended");
		return new ResponseEntityDto(true, "Notification settings updated successfully");
	}

	@Override
	public ResponseEntityDto getNotificationSettings() {
		log.info("getNotificationSettings: execution started");

		User currentUser = userService.getCurrentUser();
		Optional<User> optionalUser = userDao.findById(currentUser.getUserId());
		if (optionalUser.isEmpty()) {
			throw new ModuleException(CommonMessageConstant.COMMON_ERROR_USER_NOT_FOUND);
		}

		UserSettings userSettings = currentUser.getSettings();
		NotificationSettingsResponseDto userSettingsResponseDto = new NotificationSettingsResponseDto();

		if (userSettings != null && userSettings.getNotifications() != null) {
			JsonNode notifications = userSettings.getNotifications();

			userSettingsResponseDto.setIsLeaveRequestNotificationsEnabled(
					notifications.has(NotificationSettingsType.LEAVE_REQUEST.getKey())
							&& notifications.get(NotificationSettingsType.LEAVE_REQUEST.getKey()).asBoolean());
			userSettingsResponseDto
				.setIsTimeEntryNotificationsEnabled(notifications.has(NotificationSettingsType.TIME_ENTRY.getKey())
						&& notifications.get(NotificationSettingsType.TIME_ENTRY.getKey()).asBoolean());
			userSettingsResponseDto.setIsLeaveRequestNudgeNotificationsEnabled(
					notifications.has(NotificationSettingsType.LEAVE_REQUEST_NUDGE.getKey())
							&& notifications.get(NotificationSettingsType.LEAVE_REQUEST_NUDGE.getKey()).asBoolean());
		}
		else {
			userSettingsResponseDto.setIsLeaveRequestNotificationsEnabled(false);
			userSettingsResponseDto.setIsTimeEntryNotificationsEnabled(false);
			userSettingsResponseDto.setIsLeaveRequestNudgeNotificationsEnabled(false);
		}

		log.info("getNotificationSettings: execution ended");
		return new ResponseEntityDto(true, userSettingsResponseDto);
	}

	@Override
	public boolean isManagerAvailableForCurrentEmployee() {
		User user = userService.getCurrentUser();
		return employeeManagerDao.existsByEmployee(user.getEmployee());
	}

	@Override
	@Transactional
	public ResponseEntityDto searchEmployeesAndTeamsByKeyword(String keyword) {
		User currentUser = userService.getCurrentUser();
		log.info("searchEmployeesAndTeamsByKeyword: execution started by user: {} to search users by the keyword {}",
				currentUser.getUserId(), keyword);

		if (currentUser.getEmployee().getEmployeeRole().getAttendanceRole() == Role.ATTENDANCE_MANAGER
				|| currentUser.getEmployee().getEmployeeRole().getLeaveRole() == Role.LEAVE_MANAGER) {
			List<Team> allTeams = teamDao.findTeamsByName(keyword);
			List<EmployeeTeam> employeeTeams = employeeTeamDao.findEmployeeTeamsByEmployee(currentUser.getEmployee());
			List<EmployeeManager> employeeManagers = employeeManagerDao.findByManager(currentUser.getEmployee());

			List<Team> supervisedTeams = allTeams.stream()
				.filter(team -> employeeTeams.stream()
					.anyMatch(et -> et.getTeam().getTeamId().equals(team.getTeamId()) && et.getIsSupervisor()))
				.toList();

			List<Employee> allEmployees = employeeDao.findEmployeeByName(keyword);

			Set<Long> managedEmployeeIds = employeeManagers.stream()
				.filter(em -> em.getManagerType() == ManagerType.PRIMARY
						|| em.getManagerType() == ManagerType.SECONDARY)
				.map(em -> em.getEmployee().getEmployeeId())
				.collect(Collectors.toSet());

			Set<Long> supervisedEmployeeIds = new HashSet<>();
			boolean isSupervisor = employeeTeams.stream().anyMatch(EmployeeTeam::getIsSupervisor);

			if (isSupervisor) {
				Set<Team> supervisedTeamIds = employeeTeams.stream()
					.filter(EmployeeTeam::getIsSupervisor)
					.map(EmployeeTeam::getTeam)
					.collect(Collectors.toSet());

				List<EmployeeTeam> teamMembers = employeeTeamDao.findByTeamIn(supervisedTeamIds);
				supervisedEmployeeIds = teamMembers.stream()
					.map(et -> et.getEmployee().getEmployeeId())
					.collect(Collectors.toSet());
			}

			Set<Long> finalSupervisedEmployeeIds = supervisedEmployeeIds;
			List<Employee> filteredEmployees = allEmployees.stream()
				.filter(employee -> managedEmployeeIds.contains(employee.getEmployeeId())
						|| finalSupervisedEmployeeIds.contains(employee.getEmployeeId()))
				.toList();

			AnalyticsSearchResponseDto analyticsSearchResponseDto = new AnalyticsSearchResponseDto(
					peopleMapper.employeeListToEmployeeSummarizedResponseDto(filteredEmployees),
					peopleMapper.teamToTeamDetailResponseDto(supervisedTeams));

			log.info("searchEmployeesAndTeamsByKeyword: execution ended by user: {} to search users by the keyword {}",
					currentUser.getUserId(), keyword);
			return new ResponseEntityDto(false, analyticsSearchResponseDto);
		}

		List<Team> teams = teamDao.findTeamsByName(keyword);
		List<Employee> employees = employeeDao.findEmployeeByName(keyword);

		AnalyticsSearchResponseDto analyticsSearchResponseDto = new AnalyticsSearchResponseDto(
				peopleMapper.employeeListToEmployeeSummarizedResponseDto(employees),
				peopleMapper.teamToTeamDetailResponseDto(teams));

		log.info("searchEmployeesAndTeamsByKeyword: execution ended by user: {} to search users by the keyword {}",
				currentUser.getUserId(), keyword);
		return new ResponseEntityDto(false, analyticsSearchResponseDto);
	}

	@Override
	public ResponseEntityDto isPrimarySecondaryOrTeamSupervisor(Long employeeId) {
		User currentUser = userService.getCurrentUser();

		Optional<Employee> employeeOptional = employeeDao.findById(employeeId);
		if (employeeOptional.isEmpty()) {
			throw new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_NOT_FOUND);
		}

		List<EmployeeTeam> currentEmployeeTeams = employeeTeamDao
			.findEmployeeTeamsByEmployee(currentUser.getEmployee());
		List<EmployeeTeam> employeeTeams = employeeTeamDao.findEmployeeTeamsByEmployee(employeeOptional.get());

		PrimarySecondaryOrTeamSupervisorResponseDto primarySecondaryOrTeamSupervisor = employeeDao
			.isPrimarySecondaryOrTeamSupervisor(employeeOptional.get(), currentUser.getEmployee());

		boolean isTeamSupervisor = currentEmployeeTeams.stream()
			.anyMatch(currentTeam -> employeeTeams.stream()
				.anyMatch(empTeam -> currentTeam.getTeam().equals(empTeam.getTeam()) && currentTeam.getIsSupervisor()));

		primarySecondaryOrTeamSupervisor.setIsTeamSupervisor(isTeamSupervisor);
		return new ResponseEntityDto(false, primarySecondaryOrTeamSupervisor);
	}

	public List<EmployeeAllDataExportResponseDto> exportAllEmployeeData(List<Employee> employees,
			List<EmployeeTeamDto> teamList, List<Long> employeeIds) {
		List<EmployeeManagerDto> employeeManagerDtos = employeeDao.findManagersByEmployeeIds(employeeIds);
		List<EmployeeAllDataExportResponseDto> responseDtos = new ArrayList<>();

		for (Employee employee : employees) {
			EmployeeAllDataExportResponseDto responseDto = peopleMapper
				.employeeToEmployeeAllDataExportResponseDto(employee);
			responseDto.setJobFamily(peopleMapper.jobFamilyToJobFamilyDto(employee.getJobFamily()));
			responseDto.setJobTitle(peopleMapper.jobTitleToJobTitleDto(employee.getJobTitle()));
			responseDto.setEmployeePersonalInfoDto(
					peopleMapper.employeePersonalInfoToEmployeePersonalInfoDto(employee.getPersonalInfo()));
			responseDto.setEmployeeEmergencyDto(
					peopleMapper.employeeEmergencyToemployeeEmergencyDTo(employee.getEmployeeEmergencies()));

			List<Team> teams = teamList.stream()
				.filter(e -> Objects.equals(e.getEmployeeId(), employee.getEmployeeId()))
				.map(EmployeeTeamDto::getTeam)
				.toList();
			responseDto.setTeamResponseDto(peopleMapper.teamListToTeamResponseDtoList(teams));

			List<Employee> managers = employeeManagerDtos.stream()
				.filter(e -> Objects.equals(e.getEmployeeId(), employee.getEmployeeId()))
				.map(EmployeeManagerDto::getManagers)
				.toList();
			responseDto.setManagers(peopleMapper.employeeListToEmployeeResponseDtoList(managers));

			Optional<EmployeePeriod> period = employeePeriodDao
				.findEmployeePeriodByEmployee_EmployeeIdAndIsActiveTrue(employee.getEmployeeId());
			period.ifPresent(employeePeriod -> responseDto
				.setEmployeePeriod(peopleMapper.employeePeriodToEmployeePeriodResponseDto(employeePeriod)));

			responseDtos.add(responseDto);
		}
		return responseDtos;
	}

	public void setBulkManagers(EmployeeBulkDto employeeBulkDto, EmployeeDetailsDto employeeDetailsDto) {
		if (employeeBulkDto.getPrimaryManager() != null) {
			Optional<User> byEmail = userDao.findByEmail(employeeBulkDto.getPrimaryManager());
			if (byEmail.isPresent()) {
				Optional<Employee> managerPrimary = employeeDao.findById(byEmail.get().getUserId());
				managerPrimary.ifPresent(value -> employeeDetailsDto.setPrimaryManager(value.getEmployeeId()));
			}
		}

		if (employeeBulkDto.getSecondaryManager() != null) {
			Optional<User> byEmail = userDao.findByEmail(employeeBulkDto.getSecondaryManager());
			if (byEmail.isPresent()) {
				Optional<Employee> secondaryManager = employeeDao.findById(byEmail.get().getUserId());
				secondaryManager.ifPresent(value -> employeeDetailsDto.setSecondaryManager(value.getEmployeeId()));
			}
		}
	}

	public void setBulkEmployeeProgression(EmployeeBulkDto employeeBulkDto, Employee employee) {
		if (employeeBulkDto.getEmployeeProgression() != null) {
			EmployeeProgression employeeProgression = peopleMapper
				.employeeProgressionDtoToEmployeeProgression(employeeBulkDto.getEmployeeProgression());
			if (employeeBulkDto.getEmployeeProgression().getEmploymentType() != null) {
				employee.setEmploymentType(employeeBulkDto.getEmployeeProgression().getEmploymentType());
			}

			if (employeeBulkDto.getEmployeeProgression().getJobFamilyId() != null) {
				employeeProgression.setJobFamilyId(employeeBulkDto.getEmployeeProgression().getJobFamilyId());
			}

			if (employeeBulkDto.getEmployeeProgression().getJobTitleId() != null) {
				employeeProgression.setJobTitleId(employeeBulkDto.getEmployeeProgression().getJobTitleId());
			}

			employeeProgression.setEmployee(employee);

			if (employeeBulkDto.getEmployeeProgression().getJobTitleId() != null
					&& employeeBulkDto.getEmployeeProgression().getJobFamilyId() != null)
				employee.setEmployeeProgressions(List.of(employeeProgression));
		}
	}

	public List<EmployeeDetailedResponseDto> fetchEmployeeSearchData(Page<Employee> employees) {
		List<EmployeeDetailedResponseDto> responseDtos = new ArrayList<>();
		for (Employee employee : employees.getContent()) {

			EmployeeDetailedResponseDto responseDto = peopleMapper.employeeToEmployeeDetailedResponseDto(employee);
			responseDto.setJobFamily(peopleMapper.jobFamilyToEmployeeJobFamilyDto(employee.getJobFamily()));
			Optional<EmployeePeriod> period = employeePeriodDao
				.findEmployeePeriodByEmployee_EmployeeIdAndIsActiveTrue(employee.getEmployeeId());
			period.ifPresent(employeePeriod -> responseDto
				.setPeriodResponseDto(peopleMapper.employeePeriodToEmployeePeriodResponseDto(employeePeriod)));
			responseDtos.add(responseDto);
		}
		return responseDtos;
	}

	public void validateNIN(String nin, List<String> errors) {
		if (!nin.trim().matches(VALID_NIN_NUMBER_REGEXP))
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_NIN));

		if (nin.length() > PeopleConstants.MAX_NIN_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_NIN_LENGTH,
					new Object[] { PeopleConstants.MAX_NIN_LENGTH }));
	}

	public void validatePassportNumber(String passportNumber, List<String> errors) {
		if (passportNumber != null && (!passportNumber.trim().matches(ALPHANUMERIC_REGEX))) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_PASSPORT));
		}

		if (passportNumber != null && passportNumber.length() > PeopleConstants.MAX_NIN_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_PASSPORT_LENGTH,
					new Object[] { PeopleConstants.MAX_NIN_LENGTH }));
	}

	public void validateIdentificationNo(String identificationNo, List<String> errors) {
		if (!Validations.isValidIdentificationNo(identificationNo)) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_IDENTIFICATION_NUMBER));
		}

		if (identificationNo.length() > PeopleConstants.MAX_ID_LENGTH)
			errors
				.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_IDENTIFICATION_NUMBER_LENGTH,
						new Object[] { PeopleConstants.MAX_ID_LENGTH }));
	}

	public void validateSocialSecurityNumber(String socialSecurityNumber, List<String> errors) {
		if (socialSecurityNumber != null && (!socialSecurityNumber.trim().matches(ALPHANUMERIC_REGEX))) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_SSN));
		}

		if (socialSecurityNumber != null && socialSecurityNumber.length() > PeopleConstants.MAX_SSN_LENGTH)
			errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_EXCEEDING_MAX_CHARACTER_LIMIT,
					new Object[] { PeopleConstants.MAX_SSN_LENGTH, "First Name" }));
	}

	public void validateAddressInBulk(String addressLine, List<String> errors) {
		if (!addressLine.trim().matches(ADDRESS_REGEX))
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_ADDRESS));

		if (addressLine.length() > PeopleConstants.MAX_ADDRESS_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_ADDRESS_LENGTH,
					new Object[] { PeopleConstants.MAX_ADDRESS_LENGTH }));

	}

	public void validateStateInBulk(String state, List<String> errors) {
		if (state != null && (!state.trim().matches(SPECIAL_CHAR_REGEX))) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_CITY_STATE));
		}

		if (state != null && state.length() > PeopleConstants.MAX_ADDRESS_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_STATE_PROVINCE,
					new Object[] { PeopleConstants.MAX_ADDRESS_LENGTH }));
	}

	public void validateFirstName(String firstName, List<String> errors) {
		if (firstName != null && (!firstName.trim().matches(NAME_REGEX))) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_FIRST_NAME));
		}

		if (firstName != null && firstName.length() > PeopleConstants.MAX_NAME_LENGTH)
			errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_EXCEEDING_MAX_CHARACTER_LIMIT,
					new Object[] { PeopleConstants.MAX_NAME_LENGTH, "First Name" }));

	}

	public void validateLastName(String lastName, List<String> errors) {
		if (lastName != null && (!lastName.trim().matches(NAME_REGEX))) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_LAST_NAME));
		}

		if (lastName != null && lastName.length() > PeopleConstants.MAX_NAME_LENGTH)
			errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_EXCEEDING_MAX_CHARACTER_LIMIT,
					new Object[] { PeopleConstants.MAX_NAME_LENGTH, "Last Name" }));
	}

	public void validateEmergencyContactName(String name, List<String> errors) {
		if (name != null && (!name.trim().matches(NAME_REGEX))) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_EMERGENCY_CONTACT_NAME));
		}

		if (name != null && name.length() > PeopleConstants.MAX_NAME_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_NAME_LENGTH,
					new Object[] { PeopleConstants.MAX_NAME_LENGTH }));
	}

	public void validatePhoneNumberInBulk(String phone, List<String> errors) {
		if (phone != null && !Validations.isValidPhoneNumber(phone)) {
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_PHONE_NUMBER));
		}

		if (phone != null && phone.length() > PeopleConstants.MAX_PHONE_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_PHONE_NUMBER_LENGTH,
					new Object[] { PeopleConstants.MAX_PHONE_LENGTH }));
	}

	public void validateEmergencyContactNumberInBulk(String phone, List<String> errors) {
		if (!Validations.isValidPhoneNumber(phone)) {
			errors.add(messageUtil
				.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_EMERGENCY_CONTACT_PHONE_NUMBER));
		}
	}

	protected EmployeeBulkResponseDto createErrorResponse(EmployeeBulkDto employeeBulkDto, String message) {
		EmployeeBulkResponseDto bulkResponseDto = new EmployeeBulkResponseDto();
		bulkResponseDto.setEmail(employeeBulkDto.getWorkEmail() != null ? employeeBulkDto.getWorkEmail()
				: employeeBulkDto.getPersonalEmail());
		bulkResponseDto.setStatus(BulkItemStatus.ERROR);
		bulkResponseDto.setMessage(message);
		return bulkResponseDto;
	}

	protected List<EmployeeBulkDto> getValidEmployeeBulkDtoList(List<EmployeeBulkDto> employeeBulkDtoList) {
		return employeeBulkDtoList;
	}

	protected List<EmployeeBulkResponseDto> getTotalResultList(List<EmployeeBulkResponseDto> results,
			List<EmployeeBulkDto> overflowedEmployeeBulkDtoList) {
		if (!overflowedEmployeeBulkDtoList.isEmpty())
			throw new ModuleException(PeopleMessageConstant.PEOPLE_ERROR_EMPLOYEE_BULK_LIMIT_EXCEEDED);

		return results;
	}

	protected void updateSubscriptionQuantity(long quantity, boolean isIncrement) {
		log.info("updateSubscriptionQuantity: PRO feature {}, {}", quantity, isIncrement);
	}

	/**
	 * Validate the current user count with user limit. This method is only available for
	 * Pro tenants.
	 * @return eligibility for a new user upload.
	 */
	protected boolean checkUserCountExceeded() {
		return false;
	}

	/**
	 * Retrieves a deep copy of the given employee. This method is only available for Pro
	 * tenants.
	 * @param currentEmployee The employee to create a deep copy from.
	 * @return A deep copy of the given employee as a CurrentEmployeeDto.
	 */
	protected CurrentEmployeeDto getEmployeeDeepCopy(Employee currentEmployee) {
		return null;
	}

	/**
	 * Adds a new timeline record when a new employee is created. This feature is
	 * available only for Pro tenants.
	 * @param savedEmployee The newly saved employee entity.
	 * @param employeeDetailsDto The details of the newly created employee.
	 */
	protected void addNewEmployeeTimeLineRecords(Employee savedEmployee, CreateEmployeeRequestDto employeeDetailsDto) {
		// This feature is available only for Pro tenants.
	}

	/**
	 * Adds a new timeline record for employees who are added via quick upload. This
	 * feature is available only for Pro tenants.
	 * @param savedEmployee The employee added through quick upload.
	 * @param employeeQuickAddDto The quick-add details of the employee.
	 */
	protected void addNewQuickUploadedEmployeeTimeLineRecords(Employee savedEmployee,
			EmployeeQuickAddDto employeeQuickAddDto) {
		// This feature is available only for Pro tenants.
	}

	/**
	 * Adds new timeline records for employees who are added via bulk upload. This feature
	 * is available only for Pro tenants.
	 * @param results The employees added through bulk upload.
	 */
	protected void addNewBulkUploadedEmployeeTimeLineRecords(List<EmployeeBulkResponseDto> results) {
		// This feature is available only for Pro tenants.
	}

	/**
	 * Adds a new timeline record when an existing employee's details are updated. This
	 * feature is available only for Pro tenants.
	 * @param currentEmployee The current state of the employee before the update.
	 * @param employeeUpdateDto The updated details of the employee.
	 */
	protected void addUpdatedEmployeeTimeLineRecords(CurrentEmployeeDto currentEmployee,
			EmployeeUpdateDto employeeUpdateDto) {
		// This feature is available only for Pro tenants.
	}

	private void handleGeneralException(EmployeeBulkDto employeeBulkDto, Exception e,
			List<EmployeeBulkResponseDto> results) {
		log.warn("addEmployeeBulk: exception occurred when saving : {}", e.getMessage());
		EmployeeBulkResponseDto bulkResponseDto = createErrorResponse(employeeBulkDto, e.getMessage());
		results.add(bulkResponseDto);
	}

	private EmployeeBulkResponseDto createSuccessResponse(EmployeeBulkDto employeeBulkDto, String message) {
		EmployeeBulkResponseDto bulkResponseDto = new EmployeeBulkResponseDto();
		bulkResponseDto.setEmail(employeeBulkDto.getWorkEmail() != null ? employeeBulkDto.getWorkEmail()
				: employeeBulkDto.getPersonalEmail());
		bulkResponseDto.setStatus(BulkItemStatus.SUCCESS);
		bulkResponseDto.setMessage(message);
		return bulkResponseDto;
	}

	private void waitForTaskCompletion(List<CompletableFuture<Void>> tasks, ExecutorService executorService) {
		CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
		allTasks.thenRun(executorService::shutdown);
		allTasks.join();

		try {
			if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
				log.error("addEmployeeBulk: ExecutorService Failed to terminate after 5 minutes");
				log.error("addEmployeeBulk: Forcefully shutting down ExecutorService");
				List<Runnable> pendingTasks = executorService.shutdownNow();
				log.error("addEmployeeBulk: Found {} pending tasks while forcefully shutting down",
						pendingTasks.size());
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("addEmployeeBulk: Interrupted while waiting to terminate the ExecutorService", e);
		}
		catch (Exception e) {
			log.error("addEmployeeBulk: Error occurred while waiting to terminate the ExecutorService: {}",
					e.getMessage());
		}

		log.info("addEmployeeBulk: is executor shut down success : {}", executorService.isShutdown());
		log.info("addEmployeeBulk: all the tasks termination success after executor shut down : {}",
				executorService.isTerminated());
	}

	private int generateBulkErrorResponse(AtomicReference<ResponseEntityDto> outValues, int totalSize,
			List<EmployeeBulkResponseDto> results) {
		EmployeeBulkErrorResponseDto errorResponseDto = new EmployeeBulkErrorResponseDto();

		List<EmployeeBulkResponseDto> errorResults = results.stream()
			.filter(responseDto -> responseDto.getStatus() == BulkItemStatus.ERROR)
			.toList();

		int successCount = totalSize - errorResults.size();
		errorResponseDto.setBulkStatusSummary(new BulkStatusSummary(successCount, errorResults.size()));
		errorResponseDto.setBulkRecordErrorLogs(errorResults);
		outValues.set(new ResponseEntityDto(false, errorResponseDto));

		return successCount;
	}

	private void createNewEmployeeFromBulk(EmployeeBulkDto employeeBulkDto) {
		List<String> validationErrors = validateEmployeeBulkDto(employeeBulkDto);
		if (!validationErrors.isEmpty()) {
			throw new ValidationException(
					PeopleMessageConstant.PEOPLE_ERROR_USER_ENTITLEMENT_BULK_UPLOAD_VALIDATION_FAILED,
					validationErrors);
		}

		if (employeeBulkDto.getIdentificationNo() != null)
			employeeBulkDto.setIdentificationNo(employeeBulkDto.getIdentificationNo().toUpperCase());
		Validations.isEmployeeNameValid(employeeBulkDto.getFirstName().concat(employeeBulkDto.getLastName()));

		Employee employee = peopleMapper.employeeBulkDtoToEmployee(employeeBulkDto);
		EmployeeDetailsDto employeeDetailsDto = peopleMapper.employeeBulkDtoToEmployeeDetailsDto(employeeBulkDto);

		User user = employee.getUser();
		user.setEmail(employeeBulkDto.getWorkEmail());
		user.setIsActive(true);

		User firstUser = userDao.findById(1L)
			.orElseThrow(() -> new ModuleException(CommonMessageConstant.COMMON_ERROR_USER_NOT_FOUND));
		LoginMethod loginMethod = firstUser.getLoginMethod();

		if (loginMethod.equals(LoginMethod.GOOGLE)) {
			user.setIsPasswordChangedForTheFirstTime(true);
			user.setLoginMethod(LoginMethod.GOOGLE);
		}
		else {
			String tempPassword = CommonModuleUtils.generateSecureRandomPassword();

			user.setTempPassword(encryptionDecryptionService.encrypt(tempPassword, encryptSecret));
			user.setPassword(passwordEncoder.encode(tempPassword));
			user.setIsPasswordChangedForTheFirstTime(false);

			user.setIsPasswordChangedForTheFirstTime(false);
			user.setLoginMethod(LoginMethod.CREDENTIALS);
		}

		setBulkEmployeeProgression(employeeBulkDto, employee);
		setBulkManagers(employeeBulkDto, employeeDetailsDto);

		Set<EmployeeManager> managers = addNewManagers(employeeDetailsDto, employee);
		employee.setEmployeeManagers(managers);

		if (employeeBulkDto.getEmployeeEmergency() != null && (employeeBulkDto.getEmployeeEmergency().getName() != null
				|| employeeBulkDto.getEmployeeEmergency().getContactNo() != null)) {
			EmployeeEmergency employeeEmergency = peopleMapper
				.employeeEmergencyDtoToEmployeeEmergency(employeeBulkDto.getEmployeeEmergency());
			employeeEmergency.setEmployee(employee);
			employee.setEmployeeEmergencies(List.of(employeeEmergency));
		}

		if (employeeDetailsDto.getEmployeePersonalInfo() != null) {
			EmployeePersonalInfo employeePersonalInfo = peopleMapper
				.employeePersonalInfoDtoToEmployeePersonalInfo(employeeDetailsDto.getEmployeePersonalInfo());
			employeePersonalInfo.setEmployee(employee);
			employee.setPersonalInfo(employeePersonalInfo);
		}

		employee.setAccountStatus(employeeBulkDto.getAccountStatus());
		employee.setEmploymentAllocation(employeeBulkDto.getEmploymentAllocation());

		UserSettings userSettings = createNotificationSettingsForBulkUser(user);
		user.setSettings(userSettings);

		userDao.save(user);
		applicationEventPublisher.publishEvent(new UserCreatedEvent(this, user));

		rolesService.saveEmployeeRoles(employee);
		saveEmployeeProgression(employee, employeeBulkDto);

		if (employeeBulkDto.getTeams() != null && !employeeBulkDto.getTeams().isEmpty()) {
			saveEmployeeTeams(employee, employeeBulkDto);
		}

		if (employeeBulkDto.getEmployeePeriod() != null) {
			saveEmployeePeriod(employee, employeeBulkDto.getEmployeePeriod());
		}
	}

	private void saveEmployeeTeams(Employee employee, EmployeeBulkDto employeeBulkDto) {
		if (employeeBulkDto.getTeams() != null) {
			Set<EmployeeTeam> employeeTeams = getEmployeeTeamsByName(employeeBulkDto.getTeams(), employee);
			employeeTeamDao.saveAll(employeeTeams);
		}
	}

	private void saveEmployeeProgression(Employee employee, EmployeeBulkDto employeeBulkDto) {
		if (employeeBulkDto.getJobFamily() != null || employeeBulkDto.getJobTitle() != null
				|| employeeBulkDto.getEmployeeType() != null) {
			List<EmployeeProgression> employeeProgressions = new ArrayList<>();
			EmployeeProgression employeeProgression = new EmployeeProgression();

			if (employeeBulkDto.getJobFamily() != null && !employeeBulkDto.getJobFamily().isEmpty()) {
				JobFamily jobFamily = jobFamilyDao.getJobFamilyByName(employeeBulkDto.getJobFamily());

				if (jobFamily != null) {
					employee.setJobFamily(jobFamily);
					employeeProgression.setJobFamilyId(jobFamily.getJobFamilyId());
				}
			}

			if (employeeBulkDto.getJobTitle() != null && !employeeBulkDto.getJobTitle().isEmpty()) {
				JobTitle jobTitle = jobTitleDao.getJobTitleByName(employeeBulkDto.getJobTitle());

				if (jobTitle != null) {
					employee.setJobTitle(jobTitle);
					employeeProgression.setJobTitleId(jobTitle.getJobTitleId());
				}
			}

			if (employeeBulkDto.getEmployeeType() != null && !employeeBulkDto.getEmployeeType().isEmpty()) {
				employeeProgression.setEmploymentType(EmploymentType.valueOf(employeeBulkDto.getEmployeeType()));
			}

			employeeProgression.setEmployee(employee);
			employeeProgressions.add(employeeProgression);
			employee.setEmployeeProgressions(employeeProgressions);

			employeeDao.save(employee);
		}
	}

	private void validateMandatoryFields(EmployeeBulkDto employeeBulkDto) {
		List<String> missedFields = new ArrayList<>();

		if (employeeBulkDto.getFirstName() == null) {
			missedFields.add("First name");
		}
		if (employeeBulkDto.getLastName() == null) {
			missedFields.add("Last name");
		}

		if (employeeBulkDto.getWorkEmail() == null) {
			missedFields.add("Work Email");
		}

		if (!missedFields.isEmpty()) {
			throw new ValidationException(PeopleMessageConstant.PEOPLE_ERROR_MISSING_USER_BULK_MANDATORY_FIELDS,
					missedFields);
		}
	}

	public List<EmployeeDataExportResponseDto> exportEmployeeData(Page<Employee> employees,
			List<EmployeeTeamDto> teamList, List<Long> employeeIds) {
		List<EmployeeManagerDto> employeeManagerDtos = employeeDao.findManagersByEmployeeIds(employeeIds);
		List<EmployeeDataExportResponseDto> responseDtos = new ArrayList<>();
		for (Employee employee : employees.getContent()) {
			EmployeeDataExportResponseDto responseDto = peopleMapper.employeeToEmployeeDataExportResponseDto(employee);
			responseDto.setJobFamily(peopleMapper.jobFamilyToJobFamilyDto(employee.getJobFamily()));
			responseDto.setJobTitle(peopleMapper.jobTitleToJobTitleDto(employee.getJobTitle()));

			List<Team> teams = teamList.stream()
				.filter(e -> Objects.equals(e.getEmployeeId(), employee.getEmployeeId()))
				.map(EmployeeTeamDto::getTeam)
				.toList();

			responseDto.setTeamResponseDto(peopleMapper.teamListToTeamResponseDtoList(teams));

			List<Employee> managers = employeeManagerDtos.stream()
				.filter(e -> Objects.equals(e.getEmployeeId(), employee.getEmployeeId()))
				.map(EmployeeManagerDto::getManagers)
				.toList();
			responseDto.setManagers(peopleMapper.employeeListToEmployeeResponseDtoList(managers));
			Optional<EmployeePeriod> period = employeePeriodDao
				.findEmployeePeriodByEmployee_EmployeeIdAndIsActiveTrue(employee.getEmployeeId());
			period.ifPresent(employeePeriod -> responseDto
				.setEmployeePeriod(peopleMapper.employeePeriodToEmployeePeriodResponseDto(employeePeriod)));
			responseDtos.add(responseDto);
		}
		return responseDtos;
	}

	private TransactionTemplate getTransactionManagerTemplate() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(Propagation.REQUIRED.value());
		transactionTemplate.setIsolationLevel(Isolation.DEFAULT.value());
		return transactionTemplate;
	}

	private UserSettings createNotificationSettingsForBulkUser(User user) {
		log.info("createNotificationSettingsForBulkUser: execution started");
		UserSettings userSettings = new UserSettings();

		EmployeeRole employeeRole = rolesService.setupBulkEmployeeRoles(user.getEmployee());
		ObjectNode notificationsObjectNode = mapper.createObjectNode();

		boolean isLeaveRequestNotificationsEnabled = true;
		boolean isTimeEntryNotificationsEnabled = true;
		boolean isNudgeNotificationsEnabled = employeeRole.getIsSuperAdmin()
				|| employeeRole.getLeaveRole() == Role.LEAVE_MANAGER || employeeRole.getLeaveRole() == Role.LEAVE_ADMIN;

		notificationsObjectNode.put(NotificationSettingsType.LEAVE_REQUEST.getKey(),
				isLeaveRequestNotificationsEnabled);
		notificationsObjectNode.put(NotificationSettingsType.TIME_ENTRY.getKey(), isTimeEntryNotificationsEnabled);
		notificationsObjectNode.put(NotificationSettingsType.LEAVE_REQUEST_NUDGE.getKey(), isNudgeNotificationsEnabled);

		userSettings.setNotifications(notificationsObjectNode);
		userSettings.setUser(user);

		log.info("createNotificationSettingsForBulkUser: execution ended");
		return userSettings;
	}

	private UserSettings createNotificationSettings(EmployeeSystemPermissionsDto roleRequestDto, User user) {
		log.info("createNotificationSettings: execution started");
		UserSettings userSettings = user.getSettings();
		if (userSettings == null) {
			userSettings = new UserSettings();
		}

		ObjectNode notificationsObjectNode = mapper.createObjectNode();

		boolean isLeaveRequestNotificationsEnabled = true;
		boolean isTimeEntryNotificationsEnabled = true;
		boolean isNudgeNotificationsEnabled = roleRequestDto.getIsSuperAdmin()
				|| roleRequestDto.getLeaveRole() == Role.LEAVE_MANAGER
				|| roleRequestDto.getLeaveRole() == Role.LEAVE_ADMIN;

		notificationsObjectNode.put(NotificationSettingsType.LEAVE_REQUEST.getKey(),
				isLeaveRequestNotificationsEnabled);
		notificationsObjectNode.put(NotificationSettingsType.TIME_ENTRY.getKey(), isTimeEntryNotificationsEnabled);
		notificationsObjectNode.put(NotificationSettingsType.LEAVE_REQUEST_NUDGE.getKey(), isNudgeNotificationsEnabled);

		userSettings.setNotifications(notificationsObjectNode);
		userSettings.setUser(user);

		log.info("createNotificationSettings: execution ended");
		return userSettings;
	}

	private void validateUserEmail(String workEmail, List<String> errors) {
		if (workEmail != null && (workEmail.matches(Validation.EMAIL_REGEX))) {
			Optional<User> userBulkDtoUser = userDao.findByEmail(workEmail);
			if (userBulkDtoUser.isPresent()) {
				errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_USER_EMAIL_ALREADY_EXIST));
			}
		}
		else {
			errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_INVALID_EMAIL));
		}

		if (workEmail != null && workEmail.length() > PeopleConstants.MAX_EMAIL_LENGTH)
			errors.add(messageUtil.getMessage(CommonMessageConstant.COMMON_ERROR_VALIDATION_EMAIL_LENGTH,
					new Object[] { PeopleConstants.MAX_EMAIL_LENGTH }));
	}

	private void validateUserSupervisor(String supervisorEmail, List<String> errors) {
		if (supervisorEmail != null) {
			Optional<User> managerUser = userDao.findByEmail(supervisorEmail);
			if (managerUser.isEmpty()) {
				errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_SUPERVISOR_NOT_FOUND));
			}
			else {
				if (Boolean.FALSE.equals(managerUser.get().getEmployee().getUser().getIsActive()))
					errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_SUPERVISOR_NOT_FOUND));
				else {
					Optional<Employee> primaryManagerEmployee = employeeDao.findById(managerUser.get().getUserId());
					if (primaryManagerEmployee.isEmpty()) {
						errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_SUPERVISOR_NOT_FOUND));
					}
				}
			}
		}
	}

	private void validateCareerProgressionInBulk(EmployeeProgressionsDto employeeProgressionsDto, List<String> errors) {
		if (employeeProgressionsDto != null) {
			if (employeeProgressionsDto.getJobFamilyId() != null) {
				Optional<JobFamily> jobRole = jobFamilyDao
					.findByJobFamilyIdAndIsActive(employeeProgressionsDto.getJobFamilyId(), true);
				if (jobRole.isEmpty()) {
					errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_JOB_FAMILY_NOT_FOUND));
				}
			}
			if (employeeProgressionsDto.getJobTitleId() != null) {
				Optional<JobTitle> jobLevel = jobTitleDao
					.findByJobTitleIdAndIsActive(employeeProgressionsDto.getJobTitleId(), true);
				if (jobLevel.isEmpty()) {
					errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_JOB_TITLE_NOT_FOUND));
				}
			}
			if (employeeProgressionsDto.getStartDate() != null && employeeProgressionsDto.getEndDate() != null
					&& DateTimeUtils.isValidDateRange(employeeProgressionsDto.getStartDate(),
							employeeProgressionsDto.getEndDate())) {
				errors.add(messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_INVALID_START_END_DATE));
			}
		}
	}

	private Set<EmployeeManager> addNewManagers(EmployeeDetailsDto employeeDetailsDto, Employee finalEmployee) {
		Set<EmployeeManager> employeeManagers = new HashSet<>();

		if (employeeDetailsDto.getPrimaryManager() != null) {
			Employee manager = getManager(employeeDetailsDto.getPrimaryManager());

			if (manager != null) {
				addManagersToEmployee(manager, finalEmployee, employeeManagers, true);
			}

			if (employeeDetailsDto.getSecondaryManager() != null) {
				Employee secondaryManager = getManager(employeeDetailsDto.getSecondaryManager());
				if (manager != null && manager.equals(secondaryManager)) {
					throw new ModuleException(PeopleMessageConstant.PEOPLE_ERROR_SECONDARY_MANAGER_DUPLICATE);
				}
				addManagersToEmployee(secondaryManager, finalEmployee, employeeManagers, false);
			}
		}

		return employeeManagers;
	}

	private void addManagersToEmployee(Employee manager, Employee finalEmployee, Set<EmployeeManager> employeeManagers,
			boolean directManager) {
		EmployeeManager employeeManager = createEmployeeManager(manager, finalEmployee, directManager);
		employeeManagers.add(employeeManager);
	}

	private Employee getManager(Long managerId) {
		return employeeDao.findEmployeeByEmployeeIdAndUserActiveNot(managerId, false)
			.orElseThrow(() -> new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_MANAGER_NOT_FOUND));
	}

	private EmployeeManager createEmployeeManager(Employee manager, Employee employee, boolean directManager) {
		EmployeeManager employeeManager = new EmployeeManager();
		employeeManager.setManager(manager);
		employeeManager.setEmployee(employee);
		employeeManager.setIsPrimaryManager(directManager);
		employeeManager.setManagerType(directManager ? ManagerType.PRIMARY : ManagerType.SECONDARY);
		return employeeManager;
	}

	private void saveEmployeePeriod(Employee finalEmployee, ProbationPeriodDto probationPeriodDto) {
		EmployeePeriod employeePeriod = new EmployeePeriod();
		employeePeriod.setEmployee(finalEmployee);
		employeePeriod.setStartDate(probationPeriodDto.getStartDate());
		employeePeriod.setEndDate(probationPeriodDto.getEndDate());
		employeePeriod.setIsActive(true);
		employeePeriodDao.save(employeePeriod);
	}

	private Set<EmployeeTeam> getEmployeeTeamsByName(Set<String> teamName, Employee finalEmployee) {
		List<Team> teams = teamDao.findAllByTeamNameIn(teamName);

		if (teamName.size() != teams.size()) {
			log.info("addNewEmployee: Team ID(s) are not valid");
		}

		Set<EmployeeTeam> employeeTeams;
		if (!teams.isEmpty()) {
			employeeTeams = teams.parallelStream().map(team -> {
				EmployeeTeam employeeTeam = new EmployeeTeam();
				employeeTeam.setTeam(team);
				employeeTeam.setEmployee(finalEmployee);
				employeeTeam.setIsSupervisor(false);
				return employeeTeam;
			}).collect(Collectors.toSet());
		}
		else {
			throw new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_TEAM_NOT_FOUND);
		}
		return employeeTeams;
	}

	private List<String> validateEmployeeBulkDto(EmployeeBulkDto employeeBulkDto) {
		List<String> errors = new ArrayList<>();

		validateMandatoryFields(employeeBulkDto);

		if (employeeBulkDto.getTimeZone() != null && !employeeBulkDto.getTimeZone().isBlank()
				&& !DateTimeUtils.isValidTimeZone(employeeBulkDto.getTimeZone())) {
			throw new EntityNotFoundException(PeopleMessageConstant.PEOPLE_ERROR_INVALID_TIMEZONE);
		}

		if (employeeBulkDto.getIdentificationNo() != null)
			validateIdentificationNo(employeeBulkDto.getIdentificationNo(), errors);

		validateFirstName(employeeBulkDto.getFirstName(), errors);
		validateLastName(employeeBulkDto.getLastName(), errors);
		validateUserEmail(employeeBulkDto.getWorkEmail(), errors);
		validateUserSupervisor(employeeBulkDto.getPrimaryManager(), errors);
		validateUserSupervisor(employeeBulkDto.getSecondaryManager(), errors);
		validateCareerProgressionInBulk(employeeBulkDto.getEmployeeProgression(), errors);
		validateStateInBulk(employeeBulkDto.getEmployeePersonalInfo().getState(), errors);

		if (employeeBulkDto.getEmployeeEmergency() != null) {
			validateEmergencyContactName(employeeBulkDto.getEmployeeEmergency().getName(), errors);
			validatePhoneNumberInBulk(employeeBulkDto.getEmployeeEmergency().getContactNo(), errors);
		}
		if (employeeBulkDto.getPhone() != null)
			validatePhoneNumberInBulk(employeeBulkDto.getPhone(), errors);
		if (employeeBulkDto.getEmployeeEmergency() != null
				&& employeeBulkDto.getEmployeeEmergency().getContactNo() != null)
			validateEmergencyContactNumberInBulk(employeeBulkDto.getEmployeeEmergency().getContactNo(), errors);
		if (employeeBulkDto.getEmployeePersonalInfo().getNin() != null)
			validateNIN(employeeBulkDto.getEmployeePersonalInfo().getNin(), errors);
		if (employeeBulkDto.getAddress() != null)
			validateAddressInBulk(employeeBulkDto.getAddress(), errors);
		if (employeeBulkDto.getAddressLine2() != null)
			validateAddressInBulk(employeeBulkDto.getAddressLine2(), errors);
		validateStateInBulk(employeeBulkDto.getEmployeePersonalInfo().getCity(), errors);
		validatePassportNumber(employeeBulkDto.getEmployeePersonalInfo().getPassportNo(), errors);
		if (employeeBulkDto.getEmployeePersonalInfo().getSsn() != null) {
			validateSocialSecurityNumber(employeeBulkDto.getEmployeePersonalInfo().getSsn(), errors);
		}

		return errors;
	}

	private List<EmployeeBulkDto> getOverFlowedEmployeeBulkDtoList(List<EmployeeBulkDto> employeeBulkDtoList,
			List<EmployeeBulkDto> validList) {
		return employeeBulkDtoList.stream().filter(e -> !validList.contains(e)).toList();
	}

	private List<CompletableFuture<Void>> createEmployeeTasks(List<EmployeeBulkDto> employeeBulkDtoList,
			ExecutorService executorService, List<EmployeeBulkResponseDto> results) {
		List<CompletableFuture<Void>> tasks = new ArrayList<>();
		List<List<EmployeeBulkDto>> chunkedEmployeeBulkData = CommonModuleUtils.chunkData(employeeBulkDtoList);
		TransactionTemplate transactionTemplate = getTransactionManagerTemplate();

		String tenant = bulkContextService.getContext();

		for (List<EmployeeBulkDto> employeeBulkChunkDtoList : chunkedEmployeeBulkData) {
			for (EmployeeBulkDto employeeBulkDto : employeeBulkChunkDtoList) {
				tasks.add(createEmployeeTask(employeeBulkDto, transactionTemplate, results, executorService, tenant));
			}
		}

		return tasks;
	}

	private CompletableFuture<Void> createEmployeeTask(EmployeeBulkDto employeeBulkDto,
			TransactionTemplate transactionTemplate, List<EmployeeBulkResponseDto> results,
			ExecutorService executorService, String tenant) {
		return CompletableFuture.runAsync(() -> {
			try {
				bulkContextService.setContext(tenant);
				saveEmployeeInTransaction(employeeBulkDto, transactionTemplate);
				handleSuccessResponse(employeeBulkDto,
						messageUtil.getMessage(PeopleMessageConstant.PEOPLE_SUCCESS_EMPLOYEE_ADDED), results);
			}
			catch (DataIntegrityViolationException e) {
				handleDataIntegrityException(employeeBulkDto, e, results);
			}
			catch (Exception e) {
				handleGeneralException(employeeBulkDto, e, results);
			}
		}, executorService);
	}

	private void saveEmployeeInTransaction(EmployeeBulkDto employeeBulkDto, TransactionTemplate transactionTemplate) {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
				createNewEmployeeFromBulk(employeeBulkDto);
			}
		});
	}

	private void handleSuccessResponse(EmployeeBulkDto employeeBulkDto, String message,
			List<EmployeeBulkResponseDto> results) {
		log.warn("bulk employee added successfully : {}", employeeBulkDto.getWorkEmail());
		EmployeeBulkResponseDto bulkResponseDto = createSuccessResponse(employeeBulkDto, message);
		results.add(bulkResponseDto);
	}

	private void handleDataIntegrityException(EmployeeBulkDto employeeBulkDto, DataIntegrityViolationException e,
			List<EmployeeBulkResponseDto> results) {
		log.warn("addEmployeeBulk: data integrity violation exception occurred when saving : {}", e.getMessage());
		EmployeeBulkResponseDto bulkResponseDto = createErrorResponse(employeeBulkDto, e.getMessage());
		bulkResponseDto.setMessage(e.getMessage().contains("unique")
				? messageUtil.getMessage(PeopleMessageConstant.PEOPLE_ERROR_DUPLICATE_IDENTIFICATION_NO)
				: e.getMessage());
		results.add(bulkResponseDto);
	}

	private void updateUserStatus(Long userId, AccountStatus status, boolean isDelete) {
		log.info("updateUserStatus: execution started");

		User user = userDao.findById(userId)
			.orElseThrow(() -> new ModuleException(CommonMessageConstant.COMMON_ERROR_USER_NOT_FOUND));
		Employee employee = user.getEmployee();

		if (!Boolean.TRUE.equals(user.getIsActive())) {
			throw new ModuleException(CommonMessageConstant.COMMON_ERROR_USER_ACCOUNT_DEACTIVATED);
		}

		if (!teamDao.findTeamsManagedByUser(user.getUserId(), true).isEmpty()) {
			throw new ModuleException(CommonMessageConstant.COMMON_ERROR_TEAM_EMPLOYEE_SUPERVISING_TEAMS);
		}

		if (employeeDao.countEmployeesByManagerId(user.getUserId()) > 0) {
			throw new ModuleException(CommonMessageConstant.COMMON_ERROR_EMPLOYEE_SUPERVISING_EMPLOYEES);
		}

		List<EmployeeTeam> employeeTeams = employeeTeamDao.findEmployeeTeamsByEmployee(employee);
		employeeTeamDao.deleteAll(employeeTeams);
		employee.setEmployeeTeams(null);

		employee.setJobTitle(null);
		employee.setJobFamily(null);
		employee.setAccountStatus(status);
		employee.setTerminationDate(DateTimeUtils.getCurrentUtcDate());

		user.setIsActive(false);

		if (isDelete) {
			user.setEmail(PeopleConstants.DELETED_PREFIX + user.getEmail());
		}
		else {
			peopleEmailService.sendUserTerminationEmail(user);
		}

		userDao.save(user);
		applicationEventPublisher.publishEvent(new UserDeactivatedEvent(this, user));

		updateSubscriptionQuantity(1L, false);
		userVersionService.upgradeUserVersion(user.getUserId(), VersionType.MAJOR);
	}

}
