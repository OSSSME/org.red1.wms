/**
import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.osgi.service.event.Event;

public class WM_InOutDocEvent extends AbstractEventHandler {
 	private static CLogger log = CLogger.getCLogger(WM_InOutDocEvent.class);
		private String trxName = "";
		private PO po = null;

	@Override 
	protected void initialize() { 
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MWM_InOut.Table_Name);
		log.info("WM_InOut<PLUGIN> .. IS NOW INITIALIZED");
		}

	@Override 
	protected void doHandleEvent(Event event){
		String type = event.getTopic();
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
	}
 		else {
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
	log.info(" topic="+event.getTopic()+" po="+po);
		if (po instanceof MWM_InOut){
			if (IEventTopics.PO_AFTER_CHANGE == type){
				MWM_InOut modelpo = (MWM_InOut)po;
	log.fine("MWM_InOut changed: "+modelpo.get_ID());
	// **DO SOMETHING** ;
			}
		}
	  }
 }

	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
 	trxName = get_TrxName;
		}
}