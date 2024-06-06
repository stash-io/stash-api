package eu.tortitas.stash.jobs

import eu.tortitas.stash.EveryDayOfWeek
import eu.tortitas.stash.Scheduler
import eu.tortitas.stash.plugins.*
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.apache.commons.mail.HtmlEmail
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class ReportsJob : Runnable {
    private val application: Application

    private val smtpHost: String
    private val smtpUsername: String
    private val smtpPassword: String
    private val smtpFrom: String

    constructor(application: Application) : super() {
        val smtpHost = application.environment.config.property("mail.host").getString()
        val smtpUsername = application.environment.config.property("mail.username").getString()
        val smtpPassword = application.environment.config.property("mail.password").getString()
        val smtpFrom = application.environment.config.property("mail.from").getString()

        this.smtpHost = smtpHost
        this.smtpUsername = smtpUsername
        this.smtpPassword = smtpPassword
        this.smtpFrom = smtpFrom

        this.application = application
    }

    override fun run() {
        val userService = this.application.provideUserService()
        val linkService = this.application.provideLinkService()

        runBlocking {
            handleAllUsers(userService, linkService)
        }
    }

    private suspend fun handleAllUsers(userService: UserService, linkService: LinkService) {
        val users = userService.readAll()
        users.forEach {
            handleUser(it, linkService)
        }
    }

    private suspend fun handleUser(user: ExposedUser, linkService: LinkService) {
        if (user.reminderDayOfWeek == null) {
            return
        }

        val reminderDayOfWeek = DayOfWeek.of(user.reminderDayOfWeek!!.toInt())
        val now = ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
        if (now.dayOfWeek != reminderDayOfWeek) {
            return
        }

        val links = linkService.readByUserIdBetweenDates(
            user.id as Int,
            now.minusDays(7).format(DateTimeFormatter.ISO_DATE_TIME),
            now.format(DateTimeFormatter.ISO_DATE_TIME)
        )
        if (links.isEmpty()) {
            return
        }

        val html = createHTML().apply {
            body {
                h1 { +"Stash" }
                h2 { +"Estos son los enlaces que has guardado esta ultima semana" }

                table {
                    tr {
                        th { +"Enlace" }
                        th { +"" }
                        th { +"Descripción" }
                        th { +"" }
                        th { +"URL" }
                        th { +"" }
                        th { +"Publicado" }
                    }

                    links.map {
                        tr {
                            td { +it.title }
                            td { +"" }
                            td { +it.description!! }
                            td { +"" }
                            td { +it.url!! }
                            td { +"" }
                            td { +it.published.toString() }
                        }
                    }
                }
            }
        }

        application.environment.log.info("""
            Enviado el correo semanal a ${user.email}
        """.trimIndent())

        val email = HtmlEmail()
        email.setHostName(smtpHost)
        email.setSmtpPort(465)
        email.setAuthenticator(org.apache.commons.mail.DefaultAuthenticator(smtpUsername, smtpPassword))
        email.setSSLOnConnect(true)
        email.setFrom(smtpFrom)
        email.setSubject("Los enlaces de esta semana")
        email.setHtmlMsg(html.finalize())
        email.setTextMsg("Tu cliente de correo no soporta HTML")
        email.setCharset(org.apache.commons.mail.EmailConstants.UTF_8);
        email.addTo(user.email)
        email.send()
    }
}

fun Application.scheduleReportsJob() {
    val smtpHost = environment.config.property("mail.host").getString()
    if (smtpHost.isEmpty()) {
        environment.log.error("No se ha configurado el servidor SMTP")
        return
    }

    val scheduler = Scheduler(ReportsJob(this))

    //scheduler.scheduleExecution(Every(n = 10, unit = TimeUnit.SECONDS))
    scheduler.scheduleExecution(EveryDayOfWeek(null, hour = 9))
}