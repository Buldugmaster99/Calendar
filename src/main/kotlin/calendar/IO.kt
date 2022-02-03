package calendar

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


fun initDb() {
	Database.connect("jdbc:sqlite:data/data.sqlite")
	transaction {
		SchemaUtils.create(AppointmentTable, FileTable, NoteTable)
	}
}

fun getNotes(at: Long): List<Note> {
	return transaction {
		return@transaction Note.Notes.all().filter {
			!it.week && at == it.time
		}
	}
}

fun getWeekNotes(from: Long, to: Long): List<Note> {
	return transaction {
		return@transaction Note.Notes.all().filter {
			it.week && from < it.time && it.time < to
		}
	}
}

fun getAppointments(from: Long, to: Long): List<Appointment> {
	return transaction {
		return@transaction Appointment.Appointments.all().filter {
			from < it.start + it.duration && it.start < to
		}
	}
}

fun getWeekAppointments(): List<Appointment> {
	return transaction {
		return@transaction Appointment.Appointments.all().filter {
			it.week
		}
	}
}

object AppointmentTable: LongIdTable() {
	val start = long("start")
	val duration = long("duration")
	val title = text("title")
	val description = text("description")
	val week = bool("week")
	val type = varchar("type", 20)
}

object NoteTable: LongIdTable() {
	val time = long("time")
	val text = text("text")
	val type = varchar("type", 20)
	val week = bool("week")
//	val files = reference("files", FileTable)
}

object FileTable: LongIdTable() {
	val data = text("data")
	val name = text("text")
	val origin = text("origin")
}