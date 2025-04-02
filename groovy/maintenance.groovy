import com.pragmatix.webadmin.AdminService
import org.springframework.context.ApplicationContext

def setValidationUrl(ApplicationContext context, String[] params, PrintWriter console) {
    def adminService = context.getBean(AdminService.class)
    adminService.validationUrl = "http://aurora.rmart.ru/wormswar-admin/interop/validateTicket"
    console.print("OK. adminService.validationUrl=" + validationUrl)
}

def setValidationTicketToFalse(ApplicationContext context, String[] params, PrintWriter console) {
    def adminService = context.getBean(AdminService.class)
    adminService.validateTicket = false
    console.print("OK. adminService.validateTicket=false")
}