package duit.server.support

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.DockerClientFactory

/**
 * Testcontainers 테스트에 붙인다. 로컬에서 Docker 를 쓸 수 없으면 건너뛰고, CI 에서는 그대로 실행해 실패시킨다.
 *
 * `@Testcontainers(disabledWithoutDocker = true)` 는 CI 에서도 조용히 건너뛴다. 그래서 Testcontainers 가 Docker 29 와
 * 맞지 않아 마이그레이션 테스트가 CI 에서 돌지 않은 것을 몰랐다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DockerRequiredCondition::class)
annotation class RequiresDocker

class DockerRequiredCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult = when {
        System.getenv("CI") == "true" -> ConditionEvaluationResult.enabled("CI 에서는 Docker 가 없으면 실패해야 한다")
        DockerClientFactory.instance().isDockerAvailable -> ConditionEvaluationResult.enabled("Docker 사용 가능")
        else -> ConditionEvaluationResult.disabled("Docker 를 쓸 수 없어 건너뜀 (CI 에서는 실행)")
    }
}
