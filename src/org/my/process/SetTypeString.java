/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

	public class SetTypeString extends SvrProcess {
	private int WM_Type_ID = 0;
	private int M_Product_Category_ID = 0;
	private int M_Warehouse_ID = 0;
	private String X = "";
	private String Y = "";
	private String Z = "";
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("WM_Type_ID")){
					WM_Type_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Product_Category_ID")){
					M_Product_Category_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Warehouse_ID")){
					M_Warehouse_ID = p.getParameterAsInt();
			}
				else if(name.equals("X")){
					X = (String)p.getParameter();
			}
				else if(name.equals("Y")){
					Y = (String)p.getParameter();
			}
				else if(name.equals("Z")){
					Z = (String)p.getParameter();
			}
		}
	}
	protected String doIt() {
		return "";
	}
}
