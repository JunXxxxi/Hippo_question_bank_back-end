package com.exam.service.impl;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;

import com.exam.dao.ExroomDAO;
import com.exam.entity.Exroom;
import com.exam.service.ExamDataService;
import com.exam.service.ExroomService;
import com.exam.service.RedisService;
import com.exam.service.UserService;
import com.exam.util.FileUtil;
import com.exam.util.UpdateUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.Resource;
import org.springframework.data.domain.Pageable;
import java.io.File;
import java.util.*;

import static java.lang.Thread.sleep;

/**
 * @author junxxxxi
 * @date 2020/7/15 19:29
 **/
@Service
public class ExroomServiceimpl implements ExroomService {
    @Autowired
    UserService userService;
    @Autowired
    ExroomDAO exroomDAO;
    @Autowired
    ExamDataService examDataService;
    @Resource
    private RedisService redisService;

    @Autowired
    private RedissonClient redissonClient;//解决了使用redis分布锁的过程中，产生的业务还没执行完，但是锁到期的问题
    /**
     * 考生进入考场
     *
     * @return 0-考场不存在 1-成功 2-不在时间范围 3-用户不在允许范围
     */

    @Override
    public synchronized Map enterExroom(int kid) throws InterruptedException {
        Map ansmap = new HashMap();
        String username = userService.getusernamebysu();
        if(username.isEmpty()||username.equals("undefine")){
            ansmap.put("code","6");return ansmap;
        }

        Date now= new Date();
        Long createtime = now.getTime();

        // 给exroomInDB加上分布式锁
        RLock lock = redissonClient.getLock("exroom-"+kid);
        try{
            Exroom exroomInDB = (Exroom) redisService.get("exroom-"+kid);
            if (exroomInDB == null){
                exroomInDB= exroomDAO.findByKid(kid);
                if(exroomInDB == null){  ansmap.put("code","0");return ansmap;}//考场不存在拦截
                long time = (exroomInDB.getStarttime()+exroomInDB.getTime()*60*1000-createtime)/1000;
                redisService.set("exroom-"+kid, exroomInDB,time);
            }
            if (createtime>=exroomInDB.getDeadline()||createtime<=exroomInDB.getStarttime()){
                ansmap.put("code","2");return ansmap;}//截止时间后进入拦截
            //String uno = userService.usernametouno(username);
            Object uno = redisService.hget("TK:"+username,"uno");
            if(uno==null){ ansmap.put("code","5");return ansmap;}//学号拦截
            if((!checkpermission(String.valueOf(kid),uno.toString()))&&exroomInDB.getGrouptype()==1){
                ansmap.put("code","3");
                return ansmap;}//不在考场接受范围拦截
            if(exroomInDB.getNowStudentsNum() >= exroomInDB.getAllowStudentsNum()){
                ansmap.put("code", "7");
                return ansmap;
            } //考场参加人数达到上限
            else{
                increStuNumExroom(exroomInDB);
            }
            int ans = examDataService.addexamdata(kid,exroomInDB.getPid(),uno.toString(),exroomInDB.getAllowtimes());
            if (ans==-1||ans==2){  ansmap.put("code","4");return ansmap;}//超过考场进入上限

            //逻辑 添加考试记录 返回试题
            //  int kno = RandomUtil.toFixdLengthString(uno+kid+RandomUtil.generateDigitalString(3), 16);
            ansmap.put("code","1");
            ansmap.put("expiretime",redisService.getExpire("exroom-"+kid));
            return ansmap;
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }finally {
            if(lock.isLocked()){//判断锁是否处于锁定
                if(lock.isHeldByCurrentThread()){//判断是否时该进程自己的锁
                    lock.unlock();
                }
            }
        }


    }

    @Override
    public int startexrooom(int kid) {
        return 0;
    }

    @Override
    public int endroom(int kid) {
        return 0;
    }


    @Override
    public Page<Exroom> listexroombynum(Pageable pageable) {
        String username = userService.getusernamebysu();
        return exroomDAO.findAllByCreateBy(username,pageable);
    }
    @Override
    public Page<Exroom> listexroostu(Pageable pageable) {
        return exroomDAO.findAll(pageable);
    }
    @Override
    public List<String> stringToList(String strs){
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }
    @Override
    public Exroom findExroom(int kid) {
        Exroom exroom = (Exroom) redisService.get("exroom-"+kid);
        if (exroom == null){
            exroom= exroomDAO.findByKid(kid);
            redisService.set("exroom"+kid, exroom);
        }
        return exroom;
    }

    @Override
    public boolean isExist(int kid) {
        return false;
    }

    @Override
    public List<Exroom> listExroom() {
        return exroomDAO.findAll();
    }

    @Override
    public int addExroom(Exroom exroom) {
        Date now= new Date();
        Long createtime = now.getTime();
        exroom.setCreateTime(createtime);
        exroom.setUpdateTime(createtime);
        //    exroom.setCreateBy(userService.getusernamebysu());
        exroom.setCreateBy(userService.getusernamebysu());
        try{
            exroomDAO.save(exroom);
            return 1;
        } catch (IllegalArgumentException e){
            return 2;
        }
    }

    @Override
    public int modifyExroom(Exroom exroom) {
        Date now= new Date();
        Long updateTime = now.getTime();
        exroom.setUpdateTime(updateTime);
        int id = exroom.getKid();
        Exroom oldExroom = exroomDAO.findByKid(id);
        if(!StringUtils.isEmpty(oldExroom)){
            UpdateUtil.copyNullProperties(exroom,oldExroom);
        }
        try{
            List<Exroom> exrooms = exroomDAO.findAll();
            for(int i=0; i<exrooms.size(); i++){
                if(exroom.getKid() != exrooms.get(i).getKid()) {
                    if (exroom.getName().equals(exrooms.get(i).getName())) {
                        return 0;
                    }
                }
            }
            exroomDAO.save(exroom);
            return 1;
        }catch (IllegalArgumentException e){
            return 2;
        }
    }

    @Override
    public int delExroom(int kid) {
        if(!isExist(kid)){return -1;}
        exroomDAO.deleteById(kid);
        return 0;
    }

    @Override
    public synchronized void increStuNumExroom(Exroom exroom) throws InterruptedException {
        redisService.del("exroom-"+exroom.getKid());
        exroomDAO.updateStuNum(exroom.getKid());
        sleep(500);
        redisService.del("exroom-"+exroom.getKid());

    }

    @Override
    public List<Map<String, Object>> getExroomList() {
        return null;
    }


    @Override
    public boolean checkpermission(String exid, String uno){
        //逻辑
        //   Set<Object> set = redisService.setMembers(exid);
        //   Set<String> set_old = new HashSet<String>();
        //  set_old.add(uno);
        //   boolean ans = set.contains(uno);
        boolean ans_2 =redisService.sHasKey("EXP:"+exid, uno);
        return ans_2;
    }

    /**
     * 考生导入
     * @param exid
     * @param uno
     * @return
     */
    @Override
    public boolean putpermission(String exid,String uno){
        //Set<String> set = new HashSet<String>();
        //  set.add(uno);
        //逻辑
        boolean ans = redisService.setadd("EXP:"+exid, uno);

        return ans;
    }

    @Override
    public int uploadgrouplist(MultipartFile multipartFile) {
        File file = FileUtil.toFile(multipartFile);
        // 读取 Excel 中的数据
        ExcelReader reader = ExcelUtil.getReader(file);
        reader.readAll();
        return 0;
    }

    @Override
    public List<Exroom> getNotStartList() {
        Date now= new Date();
        Long recent = now.getTime();
        return exroomDAO.findByStarttimeAfter(recent);
    }

    @Override
    public List<Exroom> getStartedList() {
        Date now= new Date();
        Long recent = now.getTime();
        return exroomDAO.findByStarttimeBefore(recent);
    }

    @Override
    public List<Exroom> getLastThreeExam() {
        String username = userService.getusername();
        if(redisService.get("EXBY"+username)==null){
            List<Exroom> exroomList = exroomDAO.getLastThree(username);
            redisService.set("EXBY"+username,exroomList,7200);
            return exroomList;
        }
        else {
            return (List<Exroom>)redisService.get("EXBY"+username);

        }
    }

    @Override
    public List<Exroom> getSLastThreeExam() {
        if(redisService.get("NEWTEST")==null){
            List<Exroom> newexroom = exroomDAO.getSLastThree();;
            redisService.set("NEWTEST",newexroom,600);
            return newexroom;
        }
        else {
            return (List<Exroom>)redisService.get("NEWTEST");

        }

    }
}
