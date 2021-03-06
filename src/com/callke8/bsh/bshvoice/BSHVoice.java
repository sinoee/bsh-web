package com.callke8.bsh.bshvoice;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.callke8.bsh.bshcallparam.BSHCallParamConfig;
import com.callke8.system.operator.Operator;
import com.callke8.system.org.Org;
import com.callke8.utils.ArrayUtils;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.JplayerUtils;
import com.callke8.utils.MemoryVariableUtil;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;

@SuppressWarnings("serial")
public class BSHVoice extends Model<BSHVoice> {

	private static final long serialVersionUID = 1L;
	public static BSHVoice dao = new BSHVoice();
	
	public boolean add(BSHVoice voice) {
		
		boolean b = false;
		
		if(voice.save()) {
			b = true;
		}
		
		return b;
	}
	
	public boolean update(String voiceDesc,String voiceName,String voiceType,String fileName,String mimeType,String voiceId) {
		boolean b = false;
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[8];
		int index = 0;
		
		sb.append("update bsh_voice set ");
		
		if(!BlankUtils.isBlank(voiceDesc)) {
			sb.append("VOICE_DESC=?");
			pars[index] = voiceDesc;
			index++;
		}
		
		if(!BlankUtils.isBlank(voiceName)) {
			sb.append(",VOICE_NAME=?");
			pars[index] = voiceName;
			index++;
		}
		
		if(!BlankUtils.isBlank(voiceType)) {
			sb.append(",VOICE_TYPE=?");
			pars[index] = voiceType;
			index++;
		}
		
		if(!BlankUtils.isBlank(fileName)) {
			sb.append(",FILE_NAME=?");
			pars[index] = fileName;
			index++;
		}
		
		if(!BlankUtils.isBlank(mimeType)) {
			sb.append(",MIME_TYPE=?");
			pars[index] = mimeType;
			index++;
		}
		
		if(!BlankUtils.isBlank(voiceId)) {
			sb.append(" where VOICE_ID=?");
			pars[index] = voiceId;
			index++;
		}
		
		System.out.println("修改的SQL信息：" + sb.toString());
		
		System.out.println("数组信息:" + ArrayUtils.copyArray(index, pars));
		
		Object[] vs = ArrayUtils.copyArray(index, pars);
		
		System.out.println("vs.length=" + vs.length);
		
		int count = Db.update(sb.toString(), ArrayUtils.copyArray(index, pars));
		
		if(count > 0) {
			b = true;
		}
		
		return b;
		
	}
	
	public boolean delete(BSHVoice voice) {
		boolean b = false;
		
		if(BlankUtils.isBlank(voice)) {
			return b;
		}
		
		String voiceId = voice.getStr("VOICE_ID");
		
		b = deleteByVoiceId(voiceId);
		
		return b;
	}
	
	public Page<Record> getVoiceByPaginate(int pageNumber,int pageSize,String voiceDesc,String voiceType,String orgCode,String startTime,String endTime) {
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[3];
		int index = 0;
		
		sb.append("from bsh_voice where 1=1");
		
		if(!BlankUtils.isBlank(voiceDesc)) {
			sb.append(" and VOICE_DESC like ?");
			pars[index] = "%" + voiceDesc + "%";
			index++;
		}
		
		if(!BlankUtils.isBlank(voiceType) && !voiceType.equalsIgnoreCase("empty")) {
			sb.append(" and VOICE_TYPE=?");
			pars[index] = voiceType;
			index++;
		}
		
		//根据组织编码，取得所有的下属的组织编码创建的语音
		if(!BlankUtils.isBlank(orgCode)) {
			//先取出下属所有的编码
			String ocs = "";   //组织 in 的内容，即是 select * from voice where ORG_CODE in ('a','b')    
			String[] orgCodes = orgCode.split(",");   //分割组织代码
			for(String oc:orgCodes) {
				ocs += "\'" + oc + "\',";
			}
			
			ocs = ocs.substring(0,ocs.length()-1);           //去掉最后一个逗号
			System.out.println("OCS:" + ocs);
			sb.append(" and ORG_CODE in(" + ocs + ")");
		}
		
		if(!BlankUtils.isBlank(startTime)) {
			sb.append(" and CREATE_TIME>?");
			pars[index] = startTime + " 00:00:00";
			index++;
		}
		
		if(!BlankUtils.isBlank(endTime)) {
			sb.append(" and CREATE_TIME<?");
			pars[index] = endTime + "23:59:59";
			index++;
		}
		
		System.out.println("sb.toString():" + sb.toString());
		
		Page<Record> p = Db.paginate(pageNumber, pageSize, "select *", sb.toString()+" ORDER BY VOICE_INDEX ASC",ArrayUtils.copyArray(index, pars));
		
		return p;
	}
	
	public Map getVoiceByPaginateToMap(int pageNumber,int pageSize,String voiceDesc,String voiceType,String orgCode,String startTime,String endTime) {
		
		Page<Record> p = getVoiceByPaginate(pageNumber, pageSize, voiceDesc,voiceType,orgCode,startTime,endTime);
		
		int total = p.getTotalRow();   //取出总页面
		
		List<Record> newList = new ArrayList<Record>();
		int idIndex = 1;              //定义一个变量，用于生成播放器
		for(Record r:p.getList()) {
			
			String oc = r.get("ORG_CODE");   //得到组织编码
			
			Record o = Org.dao.getOrgByOrgCode(oc);  //取出组织（部门）
			if(!BlankUtils.isBlank(o)) {
				r.set("ORG_CODE_DESC", o.get("ORG_NAME"));
			}
			
			//设置操作员名字（工号）
			String uc = r.getStr("CREATE_USERCODE");   //取出创建人工号
			Operator oper = Operator.dao.getOperatorByOperId(uc);
			if(!BlankUtils.isBlank(oper)) {
				r.set("CREATE_USERCODE_DESC", oper.get("OPER_NAME") + "(" + uc + ")");
			}
			
			String vType = r.get("VOICE_TYPE").toString();    //取出语音类型
			String vTypeDesc = MemoryVariableUtil.getDictName("BSH_VOICE_TYPE",vType);   //得到语音的选项的文字
			r.set("VOICE_TYPE_DESC", vTypeDesc);
			
			//设置试听的路径
			String path =  BSHCallParamConfig.getVoicePath() + "/" + r.get("FILE_NAME") + "." + r.get("MIME_TYPE");
			
			//判断文件是否存在,如果不存在时,在输出的列表中标识一下，当客户点击试听时，提示文件不存在
			File file = new File(PathKit.getWebRootPath() + File.separator + path);
			if(file.exists()) {
				r.set("path", path);
				r.set("listenFileName", r.get("FILE_NAME") + "." + r.get("MIME_TYPE"));
			}else {
				r.set("path", "");
				r.set("listenFileName", "");
			}
			
			//设置播放器外观
			String playerSkin = JplayerUtils.getPlayerSkin(idIndex,"voice");
			r.set("playerSkin", playerSkin);
			
			System.out.println("playerSkin: ---" + playerSkin);
			
			//设置播放器函数
			String playerFunction = JplayerUtils.getPlayerFunction(idIndex, path,"voice");
			r.set("playerFunction", playerFunction);
			
			newList.add(r);
			
			idIndex++;
		}
		
		Map map = new HashMap();
		
		map.put("total",total);
		map.put("rows", newList);
		
		return map;
	}
	
	/**
	 * 根据语音ID,以 Record的方式返回语音信息
	 * 其中 Record 包括两个字段:title(标题), path(路径)
	 * 
	 * @param voiceId
	 * @param notice
	 * 			提示（如开始语音、结束语音、问题语音）
	 * @return
	 */
	public Record getVoiceForPlayListByVoiceId(String voiceId,String notice) {
		
		Record voiceRecord = new Record();
		
		BSHVoice voice = getVoiceByVoiceId(voiceId);    //根据ID，取出语音文件信息
		
		if(BlankUtils.isBlank(voice)) {
			return null;
		}
		
		//设置试听的路径
		String title = "[" + notice + "]" + voice.get("VOICE_DESC");
		String path =  MemoryVariableUtil.voicePathMap.get("autocallVoicePath") + "/" + voice.get("FILE_NAME") + "." + voice.get("MIME_TYPE");
		
		voiceRecord.set("title", title);
		voiceRecord.set("path", path);
		
		return voiceRecord;
	}
	
	/**
	 * 根据文件名,取出文件
	 * 	主要是用于事先录好的语音文件
	 * 
	 * @param fileName
	 * @param mimeType
	 * @return
	 */
	public Record getVoiceByFileName(String fileName,String mimeType) {
		Record voiceRecord = new Record();
		
		//设置试听的路径
		String title = fileName;
		String path = MemoryVariableUtil.voicePathMap.get("autocallVoicePath") + "/" + fileName + "." + mimeType;
		
		voiceRecord.set("title",title);
		voiceRecord.set("path",path);
		
		return voiceRecord;
	}
	
	/**
	 * 根据语音的ID,删除记录
	 * @param voiceId
	 * @return
	 */
	public boolean deleteByVoiceId(String voiceId) {
		
		boolean b = false;
		int count = 0;
		
		count = Db.update("delete from bsh_voice where VOICE_ID=?", voiceId);
		
		if(count>0) {
			b = true;
		}
		
		return b;
		
	}
	
	/**
	 * 根据语音描述查找语音信息
	 * 
	 * @param voiceDesc
	 * @return
	 */
	public BSHVoice getVoiceByVoiceDesc(String voiceDesc) {
		
		BSHVoice voice = findFirst("select * from bsh_voice where VOICE_DESC=?",voiceDesc);
		
		return voice;
		
	}
	
	/**
	 * 根据语音命名查找语音信息
	 * 
	 * @param voiceName
	 * @return
	 */
	public BSHVoice getVoiceByVoiceName(String voiceName) {
		BSHVoice voice = findFirst("select * from bsh_voice where VOICE_NAME=?",voiceName);
		return voice;
	}
	
	/**
	 * 取出所有的语音数据
	 * 
	 * @return
	 */
	public List<Record> getAllBSHVoice() {
		
		String sql = "select * from bsh_voice order by VOICE_ID,VOICE_INDEX ASC";
		
		List<Record> list = Db.find(sql);
		
		return list;
	}
	
	/**
	 * 根据传入的Id，取出语音记录
	 * 
	 * @param voiceId
	 * 			voiceId
	 * @return
	 */
	public BSHVoice getVoiceByVoiceId(String voiceId) {
		
		if(BlankUtils.isBlank(voiceId)) {
			return null;
		}
		
		BSHVoice voice = findFirst("select * from bsh_voice where VOICE_ID=?", voiceId);
		return voice;
	}
	
	/**
	 * 加载语音数据到内存中
	 */
	public void loadBSHVoiceDataToMemory() {
		
		List<Record> allBSHVoice = getAllBSHVoice();    //先取出所有的语音记录
		
		if(BlankUtils.isBlank(allBSHVoice)) {
			System.out.println("错误：=======-加载博世电器语音到内存失败,bsh_voice 表数据为空,请添加数据后,再重新启动进行加载!");
			return;
		}
		
		for(Record bshVoice:allBSHVoice) {				//遍历所有的语音记录,将VOICE_ID对应FILE_NAME放置MAP配置中
			
			String voiceName = bshVoice.get("VOICE_NAME");
			String fileName = bshVoice.get("FILE_NAME");
			
			BSHVoiceConfig.getVoiceMap().put(voiceName, fileName);
		}
		
	}
	
	/**
	 * 加载语音数据到内存中
	 */
	public void loadBSHVoiceDataToMemoryTest() {
		
		List<Record> allBSHVoice = getAllBSHVoice();    //先取出所有的语音记录
		
		if(BlankUtils.isBlank(allBSHVoice)) {
			System.out.println("错误：=======-加载博世电器语音到内存失败,bsh_voice 表数据为空,请添加数据后,再重新启动进行加载!");
			return;
		}
		
		for(Record bshVoice:allBSHVoice) {				//遍历所有的语音记录,将VOICE_ID对应FILE_NAME放置MAP配置中
			
			String voiceName = bshVoice.get("VOICE_NAME");
			//String fileName = bshVoice.get("FILE_NAME");
			String voiceDesc = bshVoice.get("VOICE_DESC");
			
			BSHVoiceConfig.getVoiceMap().put(voiceName, voiceDesc);
		}
		
	}
	
}
