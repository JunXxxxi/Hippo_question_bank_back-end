package com.exam.service.impl;


import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.exam.dao.PaperDAO;
import com.exam.dao.QuestionsDAO;
import com.exam.entity.Course;
import com.exam.entity.Paper;
import com.exam.entity.Questions;
import com.exam.service.CourseService;
import com.exam.service.PaperService;
import com.exam.service.QuestionsService;
import com.exam.service.UserService;
import com.exam.util.FileUtil;
import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

/**
 * @author xiaogu
 * @date 2020/7/15 19:29
 **/
@Service
public class QuestionsServiceimpl implements QuestionsService {
    @Autowired
    QuestionsDAO questionsDAO;
    @Autowired
    PaperDAO paperDAO;
    @Autowired
    PaperService paperService;
    @Autowired
    QuestionsService questionsService;
    @Autowired
    CourseService courseService;
    @Autowired
    UserService userService;
    @Override
    public List<Questions> list() {
        List<Questions> list = questionsDAO.findAll();
        return list;
    }

    @Override
    public List<Questions> listbycourse(int cid) {
        Course course = courseService.findcourse(cid);
        List<Questions> list = questionsDAO.findAllByCourse(course);
        return list;
    }

    @Override
    public int addquestion(Questions questions) {
        Date now= new Date();
        Long createtime = now.getTime();
        questions.setCreateTime(createtime);
        questions.setUpdateTime(createtime);
        String username = userService.getusernamebysu();
        questions.setCreateBy(username);
  //      if (!courseService.isexist(questions.getCid())){return 3;}
        try {
            questionsDAO.save(questions);
            return 1;
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }

    @Override
    public int modifyquestion(Questions questions) {
        Questions questionsInDB = questionsDAO.findByQid(questions.getQid());
        if (questionsInDB.equals(null)){return -1;}
        questionsInDB.setQuestionName(questions.getQuestionName());
        questionsInDB.setAnswer(questions.getAnswer());
        questionsInDB.setContext(questions.getContext());
        questionsInDB.setType(questions.getType());
        questionsInDB.setCourse(questions.getCourse());
        //questionsInDB.setCid(questions.getCid());
        questionsInDB.setDiffcult(questions.getDiffcult());
        questionsInDB.setRemarks(questions.getRemarks());
        questionsInDB.setOptionA(questions.getOptionA());
        questionsInDB.setOptionB(questions.getOptionB());
        questionsInDB.setOptionC(questions.getOptionC());
        questionsInDB.setOptionD(questions.getOptionD());
        questionsInDB.setOptionE(questions.getOptionE());
        questionsInDB.setOptionF(questions.getOptionF());
        questionsInDB.setLastmodifiedBy("user");
        Date now= new Date();
        Long modifytime = now.getTime();
        questionsInDB.setUpdateTime(modifytime);
        try{
            questionsDAO.save(questionsInDB);
            return 1;
        } catch (IllegalArgumentException e){
            return 2;
        }

    }

    @Override
    public int uploadquestion(MultipartFile multipartFile) {
        try {
            File file = FileUtil.toFile(multipartFile);
            // ?????? ExcelUtil ?????? Excel ????????????
            ExcelReader reader = ExcelUtil.getReader(file);
            // ????????? Question ??? List ?????????
            List<Map<String,Object>> questions = reader.readAll();
            // ????????????????????????
            if (!multipartFile.isEmpty()) {
                file.deleteOnExit();
            }
            for (Map<String,Object> question : questions) {
                // ????????????????????????????????????????????? ID ??????
                // ????????????
                synchronized (this) {

                    Questions newquestions = new Questions();
                    newquestions.setAnswer(question.get("??????").toString());
                    newquestions.setCourse(courseService.findcourse(Integer.parseInt(question.get("??????????????????").toString())));
                   // newquestions.setCid(Integer.parseInt(question.get("??????????????????").toString()));
                    newquestions.setRemarks(question.get("??????").toString());
                    newquestions.setQuestionName(question.get("????????????").toString());
                    newquestions.setType(Integer.parseInt(String.valueOf(question.get("????????????"))));
                    newquestions.setContext(question.get("??????").toString());
                    newquestions.setOptionA(question.get("??????A").toString());
                    newquestions.setOptionB(question.get("??????B").toString());
                    newquestions.setOptionC(question.get("??????C").toString());
                    newquestions.setOptionD(question.get("??????D").toString());
                    newquestions.setOptionE(question.get("??????E").toString());
                    newquestions.setOptionF(question.get("??????F").toString());
                   // newquestions.setDiffcult(Integer.parseInt(question.get("??????").toString()));
                        // ??????????????????
                      //  this.questionsDAO.save(newquestions);
                    System.out.println(newquestions.toString());
                }
            }
        } catch (Exception e) {
            // ????????????????????????????????????????????????????????????
           // log.error(ExceptionUtil.stacktraceToString(e));
            System.out.println(e.toString());
            return 2;
        }
        return 1;
    }

    @Override
    public int delquestion(int qid) {
        try{
        questionsDAO.deleteById(qid);
        return 1;
        } catch (IllegalArgumentException e){
            return 2;
        }
    }


    @Override
    public List<Questions> getbycidtypediff(int cid, int type, int diff) {
        return null;
    }

    @Override
    public Questions getquestionbyid(int qid) {
        Questions questions=questionsDAO.findByQid(qid);
     //   List list = new ArrayList();
  //      list.add(questions.getOptionA());
 //       list.add(questions.getOptionB());
  //      list.add(questions.getOptionC());
  //      list.add(questions.getOptionD());
//        list.add(questions.getOptionF());
      //  list.add();
        questions.setOptionList(Collections.emptyList());
        return questions;
    }

    @Override
    public List<Questions> listallbyidset(List<Integer> qidset) {
        List<Questions> questionsList = questionsDAO.findAllById(qidset);
        return questionsList;
    }

    @Override
    public Page<Questions> listqusetionbynum(Pageable pageable) {
        return questionsDAO.findAll(pageable);
    }

    @Override
    public String findquestion() {
        return null;
    }

    @Override
    public List<Questions> listbytype(int type) {

        return questionsDAO.findAllByType(type);
    }

    @Override
    public List<Questions> listbytypeandcid(int type, int cid) {
        Course course = courseService.findcourse(cid);
        return questionsDAO.findAllByTypeAndCourse(type,course);
    }

    @Override
    public List<Questions> getquestionbypid(int pid) {
        // ?????? ID ??????????????????
        Paper paper = this.paperDAO.findByPid(pid);
        // ????????????????????????????????????Example:???1,2,3,4,5,6,7???
        String qIds = paper.getQuestionId();
        // ??????????????????
        String[] ids = StrUtil.splitToArray(qIds, StrUtil.C_COMMA);
        List<Questions> questionlist = Lists.newArrayList();
        // ???????????? ID????????????????????? ID ?????????????????? Set ????????????
        for (String id : ids) {
            // ???????????? ID ?????????????????????
            Questions questions = questionsDAO.findByQid(Integer.parseInt(id));
            // ??????id??????????????????id??????????????????????????????
            questionlist.add(questions);
        }
        return questionlist;
    }

    @Override
    public List<Questions> getQuestionsRec() {
        return questionsDAO.getLastTen();
    }
}
