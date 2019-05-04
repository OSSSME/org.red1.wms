/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.process;
import java.math.BigDecimal;import java.util.ArrayList;import java.util.List;import org.compiere.model.PO;import org.compiere.model.Query;import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;import org.wms.model.MWM_HandlingUnit;

	public class GenerateHandlingUnit extends SvrProcess {
	private int Counter = 0;
	private String Prefix = "";
	int cnt = 0;	private int Capacity = 0;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("Counter")){
					Counter = p.getParameterAsInt();
			}
				else if(name.equals("Prefix")){
					Prefix = (String)p.getParameter();
			}
				else if(name.equals("Capacity")){
					Capacity = p.getParameterAsInt();
			}
		}
	}
	protected String doIt() {		for (int i=0;i<Counter;i++) {			createHandlingUnit(i+1);		}
		return "Handling Units done:"+cnt;
	}	private void createHandlingUnit(int c) { 		MWM_HandlingUnit hu = new MWM_HandlingUnit(getCtx(), 0, get_TrxName());		hu.setCapacity(new BigDecimal(Capacity));		hu.setDocStatus(MWM_HandlingUnit.DOCSTATUS_Drafted);		hu.setName(Prefix+c);		hu.saveEx(get_TrxName());		statusUpdate("Generate HandlingUnit " + hu.getName());		cnt++;	}
}
