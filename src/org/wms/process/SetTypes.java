/**



	public class SetTypes extends SvrProcess {








		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
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
				else if(name.equals("DeleteOld")){
					DeleteOld = (boolean)p.getParameterAsBoolean();
			}
		}
	}

		return "Product Types created: "+prodtypecnt+", StorageTypesCreated: "+stortypecnt;
	}
}