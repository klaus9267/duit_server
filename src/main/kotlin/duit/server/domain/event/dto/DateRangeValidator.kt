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

        // 1. 행사 종료일 > 행사 시작일
        if (request.endAt != null && request.endAt.isBefore(request.startAt)) {
            errors.add("행사 종료일은 시작일 이후여야 합니다")
        }

        // 2. 모집 종료일 > 모집 시작일
        if (request.recruitmentStartAt != null && request.recruitmentEndAt != null) {
            if (request.recruitmentEndAt.isBefore(request.recruitmentStartAt)) {
                errors.add("모집 종료일은 모집 시작일 이후여야 합니다")
            }
        }

        // 3. 모집 종료일 <= 행사 시작일 (모집은 행사 시작 전에 마감되어야 함)
        if (request.recruitmentEndAt != null && request.recruitmentEndAt.isAfter(request.startAt)) {
            errors.add("모집 종료일은 행사 시작일 이전이어야 합니다")
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

        // 1. 행사 종료일 > 행사 시작일
        if (request.endAt != null && request.endAt.isBefore(request.startAt)) {
            errors.add("행사 종료일은 시작일 이후여야 합니다")
        }

        // 2. 모집 종료일 > 모집 시작일
        if (request.recruitmentStartAt != null && request.recruitmentEndAt != null) {
            if (request.recruitmentEndAt.isBefore(request.recruitmentStartAt)) {
                errors.add("모집 종료일은 모집 시작일 이후여야 합니다")
            }
        }

        // 3. 모집 종료일 <= 행사 시작일 (모집은 행사 시작 전에 마감되어야 함)
        if (request.recruitmentEndAt != null && request.recruitmentEndAt.isAfter(request.startAt)) {
            errors.add("모집 종료일은 행사 시작일 이전이어야 합니다")
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
