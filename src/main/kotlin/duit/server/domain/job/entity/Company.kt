package duit.server.domain.job.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "companies")
class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** busino: 사업자등록번호 */
    @Column(name = "business_number", unique = true)
    var businessNumber: String? = null,

    /** corpNm: 회사명 */
    @Column(name = "corp_nm")
    var corpNm: String? = null,

    /** reperNm: 대표자명 */
    @Column(name = "reper_nm")
    var reperNm: String? = null,

    /** totPsncnt: 근로자수 */
    @Column(name = "tot_psncnt")
    var totPsncnt: Long? = null,

    /** capitalAmt: 자본금 */
    @Column(name = "capital_amt")
    var capitalAmt: Long? = null,

    /** yrSalesAmt: 연매출액 */
    @Column(name = "yr_sales_amt")
    var yrSalesAmt: Long? = null,

    /** indTpCdNm: 업종명 */
    @Column(name = "ind_tp_cd_nm")
    var indTpCdNm: String? = null,

    /** busiCont: 주요사업내용 */
    @Column(name = "busi_cont", columnDefinition = "TEXT")
    var busiCont: String? = null,

    /** corpAddr: 회사주소 */
    @Column(name = "corp_addr", columnDefinition = "TEXT")
    var corpAddr: String? = null,

    /** homePg: 회사 홈페이지 */
    @Column(name = "home_pg", length = 1000)
    var homePg: String? = null,

    /** busiSize: 회사규모 */
    @Column(name = "busi_size")
    var busiSize: String? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    val jobPostings: MutableList<JobPosting> = mutableListOf()
}
