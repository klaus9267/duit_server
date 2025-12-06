package duit.server.domain.event.dto

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class DateRangeValidator : ConstraintValidator<ValidDateRange, Any> {

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        return when (value) {
            is EventCreateRequest -> validateEventCreateRequest(value, context)
            is EventUpdateRequest -> validateEventUpdateRequest(value, context)
            else -> true
        }
    }

    private fun validateEventCreateRequest(request: EventCreateRequest, context: ConstraintValidatorContext): Boolean {
        val errors = mutableListOf<String>()

        // 1. 행사 종료일 검증
        if (request.endAt != null) {
            // 행사 종료일 >= 행사 시작일 (같은 날짜도 허용)
            if (request.endAt.isBefore(request.startAt)) {
                errors.add("행사 종료일은 시작일 이후여야 합니다")
            }
        }

        // 2. 모집 기간 검증
        if (request.recruitmentStartAt != null && request.recruitmentEndAt != null) {
            // 모집 종료일 >= 모집 시작일
            if (request.recruitmentEndAt.isBefore(request.recruitmentStartAt)) {
                errors.add("모집 종료일은 모집 시작일 이후여야 합니다")
            }
        }

        // 3. 모집 시작일만 있는 경우 (모집 종료일 없음) - 검증 불필요
        // 4. 모집 종료일만 있는 경우 (모집 시작일 없음) - 검증 불필요 (비즈니스 로직에서 처리)

        // 5. 모집 종료일과 행사 시작일 관계 검증
        if (request.recruitmentEndAt != null) {
            // 모집 종료일 <= 행사 시작일 (모집은 행사 시작 전에 마감)
            if (request.recruitmentEndAt.isAfter(request.startAt)) {
                errors.add("모집 종료일은 행사 시작일 이전이어야 합니다")
            }
        }

        // 6. 모집 시작일과 행사 시작일 관계 검증
        if (request.recruitmentStartAt != null) {
            // 모집 시작일 < 행사 시작일 (모집은 행사 시작 전에 시작)
            if (request.recruitmentStartAt.isAfter(request.startAt) ||
                request.recruitmentStartAt.isEqual(request.startAt)) {
                errors.add("모집 시작일은 행사 시작일 이전이어야 합니다")
            }
        }

        // 7. 모집 시작일과 행사 종료일 관계 검증 (모집은 행사 종료 전에 시작)
        if (request.recruitmentStartAt != null && request.endAt != null) {
            if (request.recruitmentStartAt.isAfter(request.endAt)) {
                errors.add("모집 시작일은 행사 종료일 이전이어야 합니다")
            }
        }

        // 8. 모집 종료일과 행사 종료일 관계 검증
        if (request.recruitmentEndAt != null && request.endAt != null) {
            // 모집 종료일 <= 행사 종료일
            if (request.recruitmentEndAt.isAfter(request.endAt)) {
                errors.add("모집 종료일은 행사 종료일 이전이어야 합니다")
            }
        }

        if (errors.isNotEmpty()) {
            context.disableDefaultConstraintViolation()
            errors.forEach { error ->
                context.buildConstraintViolationWithTemplate(error)
                    .addConstraintViolation()
            }
            return false
        }

        return true
    }

    private fun validateEventUpdateRequest(request: EventUpdateRequest, context: ConstraintValidatorContext): Boolean {
        val errors = mutableListOf<String>()

        // 1. 행사 종료일 검증
        if (request.endAt != null) {
            // 행사 종료일 >= 행사 시작일 (같은 날짜도 허용)
            if (request.endAt.isBefore(request.startAt)) {
                errors.add("행사 종료일은 시작일 이후여야 합니다")
            }
        }

        // 2. 모집 기간 검증
        if (request.recruitmentStartAt != null && request.recruitmentEndAt != null) {
            // 모집 종료일 >= 모집 시작일
            if (request.recruitmentEndAt.isBefore(request.recruitmentStartAt)) {
                errors.add("모집 종료일은 모집 시작일 이후여야 합니다")
            }
        }

        // 3. 모집 종료일과 행사 시작일 관계 검증
        if (request.recruitmentEndAt != null) {
            // 모집 종료일 <= 행사 시작일 (모집은 행사 시작 전에 마감)
            if (request.recruitmentEndAt.isAfter(request.startAt)) {
                errors.add("모집 종료일은 행사 시작일 이전이어야 합니다")
            }
        }

        // 4. 모집 시작일과 행사 시작일 관계 검증
        if (request.recruitmentStartAt != null) {
            // 모집 시작일 < 행사 시작일 (모집은 행사 시작 전에 시작)
            if (request.recruitmentStartAt.isAfter(request.startAt) ||
                request.recruitmentStartAt.isEqual(request.startAt)) {
                errors.add("모집 시작일은 행사 시작일 이전이어야 합니다")
            }
        }

        // 5. 모집 시작일과 행사 종료일 관계 검증
        if (request.recruitmentStartAt != null && request.endAt != null) {
            if (request.recruitmentStartAt.isAfter(request.endAt)) {
                errors.add("모집 시작일은 행사 종료일 이전이어야 합니다")
            }
        }

        // 6. 모집 종료일과 행사 종료일 관계 검증
        if (request.recruitmentEndAt != null && request.endAt != null) {
            // 모집 종료일 <= 행사 종료일
            if (request.recruitmentEndAt.isAfter(request.endAt)) {
                errors.add("모집 종료일은 행사 종료일 이전이어야 합니다")
            }
        }

        if (errors.isNotEmpty()) {
            context.disableDefaultConstraintViolation()
            errors.forEach { error ->
                context.buildConstraintViolationWithTemplate(error)
                    .addConstraintViolation()
            }
            return false
        }

        return true
    }
}
