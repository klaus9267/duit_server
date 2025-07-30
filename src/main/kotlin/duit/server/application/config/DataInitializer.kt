package duit.server.application.config

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.entity.Host
import duit.server.domain.host.repository.HostRepository
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 간호사 행사 프로젝트 개발/테스트용 초기 데이터 생성
 */
@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val hostRepository: HostRepository,
    private val eventRepository: EventRepository
) : ApplicationRunner {
    
    @Transactional
    override fun run(args: ApplicationArguments?) {
        // 테스트용 데이터들 생성 (순서 중요!)
        if (userRepository.count() == 0L) {
            createTestUsers()
        }
        
        if (hostRepository.count() == 0L) {
            createTestHosts()
        }
        
        if (eventRepository.count() == 0L) {
            createTestEvents()
        }
    }
    
    private fun createTestUsers() {
        val testUsers = listOf(
            User(
                email = "nurse.kim@hospital.com",
                nickname = "김간호사",
                providerType = ProviderType.KAKAO,
                providerId = "kakao_nurse_1"
            ),
            User(
                email = "head.nurse@medical.com", 
                nickname = "수간호사박",
                providerType = ProviderType.KAKAO,
                providerId = "google_head_nurse_2"
            ),
            User(
                email = "admin@nursingorg.com",
                nickname = "협회관리자",
                providerType = ProviderType.KAKAO,
                providerId = "kakao_admin"
            ),
            User(
                email = "icu.nurse@samsung.com",
                nickname = "중환자실이선생",
                providerType = ProviderType.KAKAO,
                providerId = "google_icu_nurse"
            ),
            User(
                email = "pediatric.nurse@asan.com",
                nickname = "소아과정간호사",
                providerType = ProviderType.KAKAO,
                providerId = "kakao_pediatric_nurse"
            ),
            User(
                email = "or.nurse@snuh.org",
                nickname = "수술실김간호사",
                providerType = ProviderType.KAKAO,
                providerId = "google_or_nurse"
            )
        )
        
        userRepository.saveAll(testUsers)
        println("✅ 테스트용 간호사 사용자 데이터 생성 완료! (${testUsers.size}개)")
    }
    
    private fun createTestHosts() {
        val testHosts = listOf(
            Host(
                name = "대한간호협회",
                thumbnail = "https://example.com/images/korea-nurses-association.png"
            ),
            Host(
                name = "서울아산병원",
                thumbnail = "https://example.com/images/asan-medical-center.png"
            ),
            Host(
                name = "삼성서울병원",
                thumbnail = "https://example.com/images/samsung-medical-center.png"
            ),
            Host(
                name = "서울대학교병원",
                thumbnail = "https://example.com/images/snuh.png"
            ),
            Host(
                name = "세브란스병원",
                thumbnail = "https://example.com/images/severance.png"
            ),
            Host(
                name = "가톨릭대학교 서울성모병원",
                thumbnail = "https://example.com/images/catholic-medical.png"
            ),
            Host(
                name = "한국간호교육학회",
                thumbnail = "https://example.com/images/nursing-education-society.png"
            ),
            Host(
                name = "대한중환자간호학회",
                thumbnail = "https://example.com/images/critical-care-nursing.png"
            ),
            Host(
                name = "분당서울대학교병원",
                thumbnail = "https://example.com/images/bundang-snuh.png"
            ),
            Host(
                name = "강남세브란스병원",
                thumbnail = "https://example.com/images/gangnam-severance.png"
            ),
            Host(
                name = "대한간호과학회",
                thumbnail = "https://example.com/images/nursing-science-society.png"
            ),
            Host(
                name = "한국보건의료인국가시험원",
                thumbnail = "https://example.com/images/kuksiwon.png"
            )
        )
        
        hostRepository.saveAll(testHosts)
        println("✅ 테스트용 의료기관/간호협회 데이터 생성 완료! (${testHosts.size}개)")
    }
    
    private fun createTestEvents() {
        val hosts = hostRepository.findAll()
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        
        val testEvents = listOf(
            // 최근 지난 이벤트들
            Event(
                title = "2024 중환자간호 최신 가이드라인",
                startAt = today.minusDays(30),
                endAt = today.minusDays(30),
                recruitmentStartAt = now.minusDays(45),
                recruitmentEndAt = now.minusDays(32),
                uri = "https://example.com/events/critical-care-guidelines-2024",
                thumbnail = "https://example.com/thumbnails/critical-care.jpg",
                isApproved = true,
                eventType = EventType.SEMINAR,
                host = hosts[7] // 대한중환자간호학회
            ),
            Event(
                title = "감염관리 실무 워크숍",
                startAt = today.minusDays(15),
                endAt = today.minusDays(13),
                recruitmentStartAt = now.minusDays(30),
                recruitmentEndAt = now.minusDays(16),
                uri = "https://example.com/events/infection-control-workshop",
                thumbnail = "https://example.com/thumbnails/infection-control.jpg",
                isApproved = true,
                eventType = EventType.WORKSHOP,
                host = hosts[1] // 서울아산병원
            ),
            
            // 진행 중인 이벤트들
            Event(
                title = "2024 간호연구 국제학술대회",
                startAt = today.minusDays(2),
                endAt = today.plusDays(3),
                recruitmentStartAt = now.minusDays(20),
                recruitmentEndAt = now.minusDays(3),
                uri = "https://example.com/events/nursing-research-conference-2024",
                thumbnail = "https://example.com/thumbnails/nursing-research.jpg",
                isApproved = true,
                eventType = EventType.CONFERENCE,
                host = hosts[10] // 대한간호과학회
            ),
            Event(
                title = "환자안전 및 의료질 향상 웨비나",
                startAt = today,
                endAt = today,
                recruitmentStartAt = now.minusDays(10),
                recruitmentEndAt = now.minusHours(2),
                uri = "https://example.com/events/patient-safety-webinar",
                thumbnail = "https://example.com/thumbnails/patient-safety.jpg",
                isApproved = true,
                eventType = EventType.WEBINAR,
                host = hosts[2] // 삼성서울병원
            ),
            
            // 모집 중인 미래 이벤트들
            Event(
                title = "신생아집중치료실 간호실무",
                startAt = today.plusDays(7),
                endAt = today.plusDays(7),
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.plusDays(5),
                uri = "https://example.com/events/nicu-nursing-practice",
                thumbnail = "https://example.com/thumbnails/nicu-nursing.jpg",
                isApproved = true,
                eventType = EventType.SEMINAR,
                host = hosts[4] // 세브란스병원
            ),
            Event(
                title = "2024 간호연구 논문 공모전",
                startAt = today.plusDays(30),
                endAt = today.plusDays(90),
                recruitmentStartAt = now,
                recruitmentEndAt = now.plusDays(25),
                uri = "https://example.com/events/nursing-research-contest",
                thumbnail = "https://example.com/thumbnails/research-contest.jpg",
                isApproved = true,
                eventType = EventType.CONTEST,
                host = hosts[0] // 대한간호협회
            ),
            Event(
                title = "수술실 간호 실무 워크숍",
                startAt = today.plusDays(14),
                endAt = today.plusDays(16),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = now.plusDays(10),
                uri = "https://example.com/events/or-nursing-workshop",
                thumbnail = "https://example.com/thumbnails/or-nursing.jpg",
                isApproved = true,
                eventType = EventType.WORKSHOP,
                host = hosts[3] // 서울대학교병원
            ),
            Event(
                title = "호스피스·완화의료 간호사 교육",
                startAt = today.plusDays(21),
                endAt = today.plusDays(21),
                recruitmentStartAt = now.plusDays(2),
                recruitmentEndAt = now.plusDays(18),
                uri = "https://example.com/events/hospice-palliative-care",
                thumbnail = "https://example.com/thumbnails/hospice-care.jpg",
                isApproved = true,
                eventType = EventType.SEMINAR,
                host = hosts[5] // 가톨릭대학교 서울성모병원
            ),
            Event(
                title = "간호교육 혁신 컨퍼런스",
                startAt = today.plusDays(35),
                endAt = today.plusDays(37),
                recruitmentStartAt = now.plusDays(5),
                recruitmentEndAt = now.plusDays(30),
                uri = "https://example.com/events/nursing-education-innovation",
                thumbnail = "https://example.com/thumbnails/nursing-education.jpg",
                isApproved = true,
                eventType = EventType.CONFERENCE,
                host = hosts[6] // 한국간호교육학회
            ),
            Event(
                title = "정신건강간호 실무 향상 세미나",
                startAt = today.plusDays(28),
                endAt = today.plusDays(28),
                recruitmentStartAt = now.plusDays(3),
                recruitmentEndAt = now.plusDays(25),
                uri = "https://example.com/events/mental-health-nursing",
                thumbnail = "https://example.com/thumbnails/mental-health.jpg",
                isApproved = true,
                eventType = EventType.SEMINAR,
                host = hosts[8] // 분당서울대학교병원
            ),
            
            // 승인 대기 중인 이벤트들
            Event(
                title = "간호사 국가시험 대비 특강",
                startAt = today.plusDays(45),
                endAt = today.plusDays(45),
                recruitmentStartAt = now.plusDays(10),
                recruitmentEndAt = now.plusDays(40),
                uri = "https://example.com/events/nursing-exam-preparation",
                thumbnail = "https://example.com/thumbnails/nursing-exam.jpg",
                isApproved = false, // 승인 대기
                eventType = EventType.SEMINAR,
                host = hosts[11] // 한국보건의료인국가시험원
            ),
            Event(
                title = "디지털헬스케어와 간호의 미래",
                startAt = today.plusDays(50),
                endAt = today.plusDays(52),
                recruitmentStartAt = now.plusDays(15),
                recruitmentEndAt = now.plusDays(45),
                uri = "https://example.com/events/digital-healthcare-nursing",
                thumbnail = "https://example.com/thumbnails/digital-health.jpg",
                isApproved = false, // 승인 대기
                eventType = EventType.CONFERENCE,
                host = hosts[9] // 강남세브란스병원
            ),
            
            // 모집 마감된 이벤트들
            Event(
                title = "심폐소생술 및 응급처치 실습",
                startAt = today.plusDays(5),
                endAt = today.plusDays(5),
                recruitmentStartAt = now.minusDays(20),
                recruitmentEndAt = now.minusDays(1),
                uri = "https://example.com/events/cpr-emergency-care",
                thumbnail = "https://example.com/thumbnails/cpr-training.jpg",
                isApproved = true,
                eventType = EventType.WORKSHOP,
                host = hosts[0] // 대한간호협회
            ),
            Event(
                title = "당뇨병 환자 간호 교육 프로그램",
                startAt = today.plusDays(12),
                endAt = today.plusDays(14),
                recruitmentStartAt = now.minusDays(15),
                recruitmentEndAt = now.minusHours(12),
                uri = "https://example.com/events/diabetes-patient-care",
                thumbnail = "https://example.com/thumbnails/diabetes-care.jpg",
                isApproved = true,
                eventType = EventType.SEMINAR,
                host = hosts[1] // 서울아산병원
            ),
            
            // 온라인 전용 이벤트들
            Event(
                title = "코로나19 이후 감염관리 대응체계",
                startAt = today.plusDays(8),
                endAt = today.plusDays(8),
                recruitmentStartAt = now.minusDays(3),
                recruitmentEndAt = now.plusDays(6),
                uri = "https://example.com/events/post-covid-infection-control",
                thumbnail = "https://example.com/thumbnails/covid-response.jpg",
                isApproved = true,
                eventType = EventType.WEBINAR,
                host = hosts[2] // 삼성서울병원
            ),
            
            // 추가 간호 전문 분야 이벤트들
            Event(
                title = "아동간호 발달 단계별 케어",
                startAt = today.plusDays(18),
                endAt = today.plusDays(18),
                recruitmentStartAt = now.plusDays(1),
                recruitmentEndAt = now.plusDays(15),
                uri = "https://example.com/events/pediatric-nursing-care",
                thumbnail = "https://example.com/thumbnails/pediatric-nursing.jpg",
                isApproved = true,
                eventType = EventType.SEMINAR,
                host = hosts[4] // 세브란스병원
            ),
            Event(
                title = "노인간호 실무 향상 과정",
                startAt = today.plusDays(25),
                endAt = today.plusDays(27),
                recruitmentStartAt = now.plusDays(3),
                recruitmentEndAt = now.plusDays(22),
                uri = "https://example.com/events/geriatric-nursing-course",
                thumbnail = "https://example.com/thumbnails/geriatric-nursing.jpg",
                isApproved = true,
                eventType = EventType.WORKSHOP,
                host = hosts[3] // 서울대학교병원
            )
        )
        
        eventRepository.saveAll(testEvents)
        println("✅ 테스트용 간호 행사 데이터 생성 완료! (${testEvents.size}개)")
        println("   🏥 간호 행사 현황:")
        println("      - 지난 행사: ${testEvents.count { it.endAt!! < today }}개")
        println("      - 진행 중인 행사: ${testEvents.count { it.startAt <= today && it.endAt!! >= today }}개") 
        println("      - 예정된 행사: ${testEvents.count { it.startAt > today }}개")
        println("      - 승인 대기: ${testEvents.count { !it.isApproved }}개")
        println("      - 모집 중: ${testEvents.count { it.recruitmentEndAt?.isAfter(now) == true && it.isApproved }}개")
        println("   📚 행사 유형별:")
        println("      - 세미나: ${testEvents.count { it.eventType == EventType.SEMINAR }}개")
        println("      - 워크숍: ${testEvents.count { it.eventType == EventType.WORKSHOP }}개")
        println("      - 컨퍼런스: ${testEvents.count { it.eventType == EventType.CONFERENCE }}개")
        println("      - 웨비나: ${testEvents.count { it.eventType == EventType.WEBINAR }}개")
        println("      - 공모전: ${testEvents.count { it.eventType == EventType.CONTEST }}개")
    }
}
