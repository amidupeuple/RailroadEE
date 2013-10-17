import common.Constants;
import dao.StationInRouteDAO;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.ScheduleDTO;
import exceptions.EntityUpdateException;
import exceptions.GetScheduleException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

@Stateless(name = "ScheduleEJB")
public class ScheduleBean{

    @EJB
    StationInRouteDAO sirDAO;

    //Get trains schedule from station A to station B in the given time interval. All required data for this service
    //is encapsulated in parameter reqObj.
    public ResponseDTO scheduleFromAtoB(RequestDTO reqObj) {
        List<ScheduleDTO> scheduleList;

        ScheduleDTO userRequirements = (ScheduleDTO) reqObj.getObject();

        try {
            scheduleList = sirDAO.getScheduleFromAtoB(userRequirements);
        } catch (GetScheduleException ex) {
            return new ResponseDTO(Constants.StatusOfExecutedService.error, ex.getMessage());
        }

        return new ResponseDTO(Constants.StatusOfExecutedService.success, scheduleList);
    }

    //Get schedule for station.
    public ResponseDTO scheduleForStation(RequestDTO reqObj) {
        List<ScheduleDTO> scheduleList;

        ScheduleDTO userRequirements = (ScheduleDTO) reqObj.getObject();

        try {
            scheduleList = sirDAO.getScheduleForStation(userRequirements);
        } catch (GetScheduleException ex) {
            return new ResponseDTO(Constants.StatusOfExecutedService.error, ex.getMessage());
        }

        return new ResponseDTO(Constants.StatusOfExecutedService.success, scheduleList);
    }

    /**
     * This service add in DB new route, defines stations that make up this route, set particular train to this route.
     * @param reqObj - it contains as object list of ScheduleDTO instances - stations in new route.
     * @return result of this service: state and explanatory message.
     */
    public ResponseDTO addRoute(RequestDTO reqObj) {

        List<ScheduleDTO> userRequirements = (List<ScheduleDTO>) reqObj.getObject();

        try {
            sirDAO.addRoute(userRequirements);
        } catch (EntityUpdateException e) {
            return new ResponseDTO(Constants.StatusOfExecutedService.error, e.getMessage());
        }

        return new ResponseDTO(Constants.StatusOfExecutedService.success, "Новый маршрут добавлен успешно");
    }



    public String getHello() {
        return "Hello";
    }
}
