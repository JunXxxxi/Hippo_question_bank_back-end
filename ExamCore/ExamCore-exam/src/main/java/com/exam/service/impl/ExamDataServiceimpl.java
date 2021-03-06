package com.exam.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.exam.dao.ExamdataDAO;
import com.exam.dao.PaperDAO;
import com.exam.dto.ResultDTO;
import com.exam.entity.Answer;
import com.exam.entity.Examdata;
import com.exam.entity.Paper;
import com.exam.entity.Questions;
import com.exam.service.AnswerService;
import com.exam.service.ExamDataService;
import com.exam.service.RedisService;
import com.exam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author junxxxxi
 * @date 2020/7/17 17:23
 **/
@Service
public class ExamDataServiceimpl implements ExamDataService {
    @Autowired
    ExamdataDAO examdataDAO;
    @Autowired
    RedisService redisService;
    @Autowired
    PaperDAO paperDAO;
    @Autowired
    UserService userService;
    @Autowired
    AnswerService answerService;
    @Override
    public synchronized int addexamdata(int kid,int pid,String uno,int ltimes) {
        Examdata examdata = getexam(kid, pid, uno);

        int times = examdata.getTimes();
        if (times>=ltimes&&ltimes!=0){return -1;}
        if (times==0){
            examdata.setTimes(1);
        }else {
            examdata.setTimes(times+1);
        }
        Date now= new Date();
        Long createtime = now.getTime();
        examdata.setKid(kid);
        examdata.setPid(pid);
        examdata.setUno(uno);
        examdata.setCreateTime(createtime);
        examdata.setStatus(1);
        try{
            examdataDAO.save(examdata);
            return 1;
        } catch (IllegalArgumentException e){
            return 2;
        }

    }

    @Override
    public Examdata getexam(int kid,int pid,String uno) {
        Examdata examdata = examdataDAO.findByKidAndPidAndUno(kid, pid, uno);
        if(examdata==null){return new Examdata();}
        else {
            return examdata;
        }
    }



    @Override
    public int updateexamdata(int kid, int pid, String uno, Map<String,String> ans) {
        Examdata examdata = examdataDAO.findByKidAndPidAndUno(kid, pid,uno);
        if (examdata==null){return -1; }
        int eid = examdata.getEid();
        int times = examdata.getTimes();
        if(times==1){
            int i=1;
            for(Map.Entry<String, String> entry : ans.entrySet()){
                int mapKey = Integer.parseInt(entry.getKey());
                String mapValue = entry.getValue();
                Answer answer = new Answer();
                answer.setEid(eid);
                answer.setQid(mapKey);
                answer.setAnswerContent(mapValue);
                answer.setVisualQid(i);
                i++;
                answerService.saveAnswer(answer);
            }
        }
        else {
            for(Map.Entry<String, String> entry : ans.entrySet()){
                int mapKey = Integer.parseInt(entry.getKey());
                String mapValue = entry.getValue();
                answerService.upadteAnswer(eid,mapKey,mapValue);
            }
        }
      //  examdata.setTotalscore(score);
     //   examdata.setWronglist(wronglist);

        Date now= new Date();
        Long createtime = now.getTime();
        examdata.setUpdateTime(createtime);
        examdata.setStatus(2);
        try{
            examdataDAO.save(examdata);
            return 1;
        } catch (IllegalArgumentException e){
            return 2;
        }
    }

    @Override
    public int updatescore(int kid, int pid, String uno, Map<String, String> ans, float score, String wronglist) {
        Examdata examdata = examdataDAO.findByKidAndPidAndUno(kid, pid,uno);
        if (examdata==null){return -1; }
          examdata.setTotalscore(score);
          examdata.setWronglist(wronglist);
        Date now= new Date();
        Long createtime = now.getTime();
        examdata.setUpdateTime(createtime);
        examdata.setStatus(3);
        try{
            examdataDAO.save(examdata);
            return 1;
        } catch (IllegalArgumentException e){
            return 2;
        }
    }

    @Override
    public boolean delexamdata(int eid) {
        examdataDAO.deleteById(eid);
        return true;
    }

    @Override
    public int modifydata(Examdata examdata) {
        return 0;
    }

    @Override
    public List<Examdata> querydatabyuno() {
        String username = userService.getusernamebysu();
        String uno = userService.getUnoByUsername(username);
        return examdataDAO.findAllByUno(uno);
    }

    @Override
    public List<Examdata>  querydatabykid(int kid) {
        return examdataDAO.findAllByKid(kid);
    }

    @Override
    public Map getExamResult(int kid) {
        String username = userService.getusernamebysu();
        String uno = userService.getUnoByUsername(username);
        Examdata examdata;
        try{
            examdata = examdataDAO.findByKidAndUno(kid,uno);
        }catch (Exception e){
            return null;
        }
        int pid = examdata.getPid();
        Paper paper = findPaperbyid(pid);
        Map<String, Object> paperinfo = new HashMap();
        paperinfo.put("papername", paper.getName());
        float fullmark = paperDAO.getTotalScoreByPid(pid);
        paperinfo.put("fullmark",fullmark);
        float yourscore = examdata.getTotalscore();
        paperinfo.put("yourscore",yourscore);
        List<Questions> questionSet = JSONObject.parseObject(paper.getQucontent(),List.class);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("count", questionSet.size());
        map.put("questionList", questionSet);
        String json = JSON.toJSONString(map, true);
        List<Questions> questionList = JSON.parseArray(JSON.parseObject(json).getString("questionList"),Questions.class);
        List<ResultDTO> resultDTOS =questionList.stream().map(questions ->(ResultDTO) new ResultDTO().convertFrom(questions)).collect(Collectors.toList());
        paperinfo.put("questions",resultDTOS);
        redisService.hmset("psh-"+pid,paperinfo,3600);
        // long time = redisService.getExpire("exroom-"+kid)
        int eid = examdata.getEid();
        paperinfo.put("youranswer",answerService.getAnswer(eid));
        return paperinfo;
    }

    @Override
    public Map getTExamResult(int kid, String uno) {
        Examdata examdata;
        try{
            examdata = examdataDAO.findByKidAndUno(kid,uno);
        }catch (Exception e){
            return null;
        }
        int pid = examdata.getPid();
        Paper paper = findPaperbyid(pid);
        Map<String, Object> paperinfo = new HashMap();
        paperinfo.put("papername", paper.getName());
        float fullmark = paperDAO.getTotalScoreByPid(pid);
        paperinfo.put("fullmark",fullmark);
        float yourscore = examdata.getTotalscore();
        paperinfo.put("yourscore",yourscore);
        List<Questions> questionSet = JSONObject.parseObject(paper.getQucontent(),List.class);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("count", questionSet.size());
        map.put("questionList", questionSet);
        String json = JSON.toJSONString(map, true);
        List<Questions> questionList = JSON.parseArray(JSON.parseObject(json).getString("questionList"),Questions.class);
        List<ResultDTO> resultDTOS =questionList.stream().map(questions ->(ResultDTO) new ResultDTO().convertFrom(questions)).collect(Collectors.toList());
        paperinfo.put("questions",resultDTOS);
        redisService.hmset("psh-"+pid,paperinfo,3600);
        // long time = redisService.getExpire("exroom-"+kid)
        int eid = examdata.getEid();
        paperinfo.put("youranswer",answerService.getAnswer(eid));
        return paperinfo;
    }

    public Paper findPaperbyid(int pid) {
        Object paperdb = redisService.get("ppif-"+pid);
        if (paperdb==null){
            Paper paperdbdao = paperDAO.findByPid(pid);
            String json = JSON.toJSONString(paperdbdao, true);
            redisService.set("ppif-"+pid,json,3600);
            return paperdbdao;
        } else {
            Paper paper = JSONObject.parseObject(paperdb.toString(),Paper.class);
            return paper;
        }

    }
}
