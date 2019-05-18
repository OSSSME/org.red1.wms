/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.process;
import java.math.BigDecimal;import java.util.ArrayList;import java.util.List;import org.compiere.model.PO;import org.compiere.model.Query;import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;import org.compiere.util.Env;import org.wms.model.MWM_HandlingUnit;

	public class GenerateHandlingUnit extends SvrProcess {
	private Integer Counter = 0;
	String name = "";	private String Prefix = "";
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
	protected String doIt() {		//leading zeros string from Capacity/10		int leading = Counter.toString().length();		StringBuilder zeros = new StringBuilder();		for (int i=0;i<leading;i++) {			zeros.append("0");		} 		//find last number		int last = 0;		MWM_HandlingUnit lasthu = new Query(getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_Name+" Like '"+Prefix+"%'",get_TrxName())				.setOrderBy("Created DESC").first();		if (lasthu!=null) {			String lastnumber = lasthu.getName().substring(lasthu.getName().length()-6);			last = Integer.valueOf(lastnumber);		}		for (int i=0;i<Counter;i++) {			createHandlingUnit(leading+3,last++); 			cnt++;			if (cnt>1000)				break;		}
		return "Handling Units done:"+cnt+ " Last Unit: "+name;
	}	private void createHandlingUnit(int zeros,int serial) { 		MWM_HandlingUnit hu = new MWM_HandlingUnit(getCtx(), 0, get_TrxName());		hu.setCapacity(new BigDecimal(Capacity));		hu.setDocStatus(MWM_HandlingUnit.DOCSTATUS_Drafted); 		hu.setName(Prefix+String.format("%0"+zeros+"d", serial));		hu.setQtyMovement(Env.ZERO);		hu.saveEx(get_TrxName());		name = hu.getName();		statusUpdate("Generate HandlingUnit " + name); 	}
}
