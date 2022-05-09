package frame


import calendar.Appointment
import calendar.Reminder
import calendar.Timing
import frame.TabManager.Secure
import frame.popup.AppointmentPopup
import frame.popup.ReminderPopup
import frame.styles.GlobalStyles
import frame.styles.MenubarStyles
import frame.styles.NoteStyles
import frame.styles.OverviewStyles
import frame.styles.ReminderStyles
import frame.styles.TabStyles
import frame.styles.WeekStyles
import frame.tabs.createOverviewTab
import frame.tabs.createReminderTab
import init
import javafx.application.*
import javafx.beans.property.*
import javafx.beans.value.*
import javafx.event.*
import javafx.scene.control.*
import javafx.scene.image.*
import javafx.scene.layout.*
import javafx.stage.*
import logic.Configs
import logic.Exit
import logic.Language
import logic.LogType
import logic.Warning
import logic.getConfig
import logic.getLangString
import logic.log
import logic.translate
import org.controlsfx.control.ToggleSwitch
import tornadofx.*
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import javax.imageio.IIOException
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.reflect.KFunction



//https://edvin.gitbooks.io/tornadofx-guide/content/part1/7_Layouts_and_Menus.html

/**
 * BLOCKING
 */
fun frameInit() {
	createLoading()
	log("created loading", LogType.NORMAL)
	
	/**
	 * this looks pretty weird, but it essentially
	 * creates a stacktrace with the head of an Exit
	 * and the StackTrace of the actual exception
	 *
	 * /*
	 * if it is not an Exit(custom Exception) then
	 * Exit<errorCode> -> Exception is added at the beginning
	 * */
	 *
	 * @see Exit
	 */
	DefaultErrorHandler.filter = {
		val writer = StringWriter()
		
		//if(it.error::class != Exit::class) {
		writer.append("Exit <ErrorCode: Frame Exception> -> ")
		//}
		
		if(getConfig(Configs.PrintStacktrace))
			it.error.printStackTrace(PrintWriter(writer))
		else
			writer.append(it.error.toString())
		log(writer, LogType.ERROR)
		
		// uncomment if errorPopup should be disabled  enable in Release
		// it.consume()
	}
	
	log("launching Application", LogType.IMPORTANT)
	launch<Window>()
	//LauncherImpl.launchApplication(Window::class.java, PreloaderWindow::class.java, emptyArray())
}

class Window: App(
	MainView::class,
	GlobalStyles::class, MenubarStyles::class, TabStyles::class,
	NoteStyles::class, ReminderStyles::class, OverviewStyles::class,
	WeekStyles::class
) {
	
	override fun start(stage: Stage) {
		stage.height = 600.0
		stage.width = 800.0
		super.start(stage)
		removeLoading()
		log("started Frame", LogType.NORMAL)
		TabManager.openTab("calendar", ::createOverviewTab)
		TabManager.openTab("reminders", ::createReminderTab)
		
	}
}

class MainView: View("Calendar") {
	override val root = borderpane {
		top = createMenuBar(this)
		log("created menubar", LogType.IMPORTANT)
		
		center = tabpane {
			tabDragPolicy = TabPane.TabDragPolicy.REORDER
			tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS
			TabManager.pane = this@tabpane
		}
		log("created pane", LogType.IMPORTANT)
	}
}

fun createMenuBar(pane: BorderPane): MenuBar {
	return pane.menubar {
		menu(getLangString("create")) {
			createMenuGroup(
				createMenuItem(this@menu, "Appointment", "Strg + N") {
					AppointmentPopup.open(getLangString("new appointment"), getLangString("create"),
						false,
						null,
						Timing.getNowLocal(),
						Timing.getNowLocal().plusHours(1),
						save = { app: Appointment ->
							log("Created:$app")
						}
					)
				},
				createMenuItem(this@menu, "Reminder", "Strg + R") {
					ReminderPopup.open(getLangString("new reminder"), getLangString("Create"),
						false,
						null,
						Timing.getNowLocal(),
						save = { rem: Reminder ->
							log("Created:$rem")
						}
					)
				}
			)
		}
		menu(getLangString("options")) {
			createMenuGroup(
				createMenuItem(this@menu, "Reload", "F5") {
					init()
					Secure.overrideTab("calendar", ::createOverviewTab)
				},
				createMenuItem(this@menu, "Preferences", "Strg + ,") { log("Preferences") },
				run { separator(); return@run null },
				createMenuItem(this@menu, "Quit", "Strg + Q") {
					log("exiting Program via quit", LogType.IMPORTANT)
					Platform.exit()
				}
			)
		}
		menu(getLangString("view")) {
			createMenuGroup(
				createMenuItem(this@menu, "Show Reminder", "Strg + Shift + R") {
					log("Show Reminder")
					Secure.overrideTab("reminders", ::createReminderTab)
				},
				createMenuItem(this@menu, "Show Calendar", "Strg + Shift + C") {
					log("Show Calendar")
					Secure.overrideTab("calendar", ::createOverviewTab)
				}
			)
		}
		menu(getLangString("help")) {
			createMenuGroup(
				createMenuItem(this@menu, "Github", "") {
					log("Open Github", LogType.IMPORTANT)
					try {
						runAsync {
							Desktop.getDesktop().browse(URI("https://github.com/Buldugmaster99/Calendar"))
						}
					} catch(e: IOException) {
						log("failed to open browser $e", LogType.WARNING)
					}
				},
				createMenuItem(this@menu, "Memory Usage", "") {
					//System.gc()
					val rt = Runtime.getRuntime()
					val usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
					log("used memory: $usedMB  | max memory: ${rt.maxMemory() / 1024 / 1024}  | total memory ${rt.totalMemory() / 1024 / 1024}  | free memory ${rt.freeMemory() / 1024 / 1024}")
				},
				run { separator(); return@run null },
				createMenuItem(this@menu, "Help", "") {
					log("Help")
				}
			)
		}
	}
}

fun createMenuGroup(vararg panes: GridPane?) {
	log("creating MenuGroup with ${panes.size} elements", LogType.LOW)
	var currentWidth = 10.0
	val items = panes.filterNotNull()
	val changed = mutableListOf<GridPane>()
	items.forEach { item ->
		item.apply {
			widthProperty().addListener(ChangeListener { _, _, newWidth ->
				if(!changed.contains(this))
					changed.add(this)
				if(newWidth.toDouble() > currentWidth)
					currentWidth = newWidth.toDouble()
				if(changed.size == items.size)
					items.forEach {
						it.prefWidth = currentWidth
					}
			})
		}
	}
}

fun createMenuItem(menu: Menu, name: String, shortcut: String, action: () -> Unit): GridPane? {
	log("creating menuItem: $name $shortcut", LogType.LOW)
	var grid: GridPane? = null
	menu.customitem {
		grid = gridpane {
			addClass(MenubarStyles.gridPane_)
			
			label(getLangString(name)) {
				addClass(MenubarStyles.itemName_)
				gridpaneConstraints {
					columnRowIndex(0, 0)
				}
			}
			label {
				addClass(MenubarStyles.spacing_)
				gridpaneConstraints {
					columnRowIndex(1, 0)
					hGrow = Priority.ALWAYS
				}
			}
			label(shortcut) {
				addClass(MenubarStyles.shortcut_)
				gridpaneConstraints {
					columnRowIndex(2, 0)
				}
			}
		}
		action(action)
	}
	
	return grid
}

/**
 * Manges tabs for the frame and is used to
 * create, override and close tabs
 *
 * it contains another object with
 * methods that must be handled with care
 *
 * @see Secure
 */
object TabManager {
	
	lateinit var pane: TabPane
	
	/**
	 * <identifier, Tab>
	 */
	private val tabs: MutableMap<String, Tab> = mutableMapOf()
	
	/**
	 * creates a new tab, or focuses the tab if it already exists
	 *
	 * takes an identifier, a function to create the tab and arguments for that function
	 * as arguments
	 *
	 * adds the tab to the pane if new created and sets focus on that tab
	 *
	 * @param identifier this should be a unique identifier for the tab
	 *                   like the title but with some extra information
	 *                   so that it doesn't prevent another tab from getting
	 *                   created and instead joinks focus
	 *
	 *                   Examples:
	 *                   > "DayNotes2020/6/12"
	 *                   > "Week2012/43"
	 *                   > "Settings"
	 *
	 * @param createFunction the function to create the tab
	 *
	 *                       must take a pane as its first parameter
	 *                       and return ab Tab
	 *
	 *                       Examples:
	 *                       > ::createWeekTab(fun createWeekTab(pane: TabPane, week: Week, day: Day?): Tab)
	 *                       > ::createNoteTab(fun createNoteTab(pane: TabPane, cell: Celldisplay): Tab)
	 *
	 * @param methodArgs add all extra parameters apart from the pane here
	 *
	 *                       Examples:
	 *                       > week,day
	 *                       > data
	 */
	fun openTab(identifier: String, createFunction: KFunction<Tab>, vararg methodArgs: Any?) {
		val newTab = tabs.getOrElse(identifier) { createFunction.call(pane, *methodArgs).also { tabs[identifier] = it } }
		newTab.setOnClosed { tabs.remove(identifier) }
		
		pane.selectionModel.select(newTab)
	}
	
	/**
	 * this part contains methods
	 * for the Tabmanager that should
	 * be handled carefully.
	 */
	object Secure {
		@Suppress("unused")
		fun closeTab(identifier: String) {
			tabs[identifier]?.close()
			tabs.remove(identifier)
		}
		
		fun overrideTab(identifier: String, createFunction: KFunction<Tab>, vararg methodArgs: Any?) {
			tabs[identifier]?.close()
			
			val tab = createFunction.call(pane, *methodArgs).also { tabs[identifier] = it }
			tab.setOnClosed { tabs.remove(identifier) }
			
			pane.selectionModel.select(tab)
		}
	}
	
	override fun toString(): String = tabs.keys.toString()
	
}


/**
 * <path,image>
 */
val cache = mutableMapOf<String, Image>()

fun createFXImage(name: String): Image {
	val path = "img/$name"
	cache[path]?.let { return it }
	
	val image = try {
		ImageIO.read({}::class.java.classLoader.getResource(path))
	} catch(e: IllegalArgumentException) {
		Warning("imageError", e, "file not found:$path")
		getImageMissing()
	} catch(e: IIOException) {
		Warning("imageError", e, "can't read file:$path")
		getImageMissing()
	}
	val wr = WritableImage(image.width, image.height)
	val pw = wr.pixelWriter
	for(x in 0 until image.width) {
		for(y in 0 until image.height) {
			pw.setArgb(x, y, image.getRGB(x, y))
		}
	}
	cache[path] = wr
	return wr
}

fun getImageMissing(): BufferedImage {
	val im = BufferedImage(30, 30, BufferedImage.TYPE_3BYTE_BGR)
	val g2 = im.graphics
	for(i in 0 until 15)
		for(j in 0 until 15) {
			g2.color = java.awt.Color(Random.nextInt(255), Random.nextInt(155), Random.nextInt(155))
			g2.fillRect(1 + i * 2, 1 + j * 2, 3, 3)
		}
	for(i in 0 until 30) {
		g2.color = java.awt.Color(200, 10, 50)
		g2.fillRect(i, 0, 1, 1)
		g2.fillRect(0, i, 1, 1)
		g2.fillRect(29, i, 1, 1)
		g2.fillRect(i, 29, 1, 1)
		g2.color = java.awt.Color(200, 180, 200)
		g2.fillRect(i, 1, 1, 1)
		g2.fillRect(1, i, 1, 1)
		g2.fillRect(28, i, 1, 1)
		g2.fillRect(i, 28, 1, 1)
	}
	
	g2.dispose()
	return im
}


fun EventTarget.toggleSwitch(
	text: ObservableValue<String>? = null,
	selected: Property<Boolean> = true.toProperty(),
	op: ToggleSwitch.() -> Unit = {}
) = ToggleSwitch().attachTo(this, op) {
	it.selectedProperty().bindBidirectional(selected)
	if(text != null)
		it.textProperty().bind(text)
}


class TranslatingSimpleStringProperty(initialValue: String = "", private val type: Language.TranslationTypes, private vararg val args: Any): SimpleStringProperty(initialValue) {
	override fun set(newValue: String?) {
		super.set(newValue)
	}
	
	override fun get(): String = super.get().translate(type, args)
	
}