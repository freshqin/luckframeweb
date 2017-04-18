package luckyweb.seagull.spring.dao;

import java.sql.SQLException;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import luckyweb.seagull.comm.QueueListener;
import luckyweb.seagull.quartz.QuratzJobDataMgr;
import luckyweb.seagull.spring.entity.TestJobs;
import luckyweb.seagull.util.StrLib;

@Repository("testJobsDao")
public class TestJobsDaoImpl extends HibernateDaoSupport implements TestJobsDao {
	private static final Logger log = Logger.getLogger(TestJobsDaoImpl.class);

	@Resource(name = "sessionFactory")
	public void setSuperSessionFactory(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	public static boolean isRun = false;

	/**
	 * 启动 定时任务 ，修改状态为启动 state=1
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void init(Session session) throws Exception {
		QueueListener.gSchedulerFactory = new StdSchedulerFactory();
		QueueListener.sched = QueueListener.gSchedulerFactory.getScheduler();
		
		if (isRun == false) {
			try {
				Query queryproj = session.createQuery("select  projectid, projectname from SectorProjects where projecttype=1 order by projectid asc");
				QueueListener.projlist = queryproj.list();//获取所有项目
				
				Query query = session.createQuery("from TestJobs where (tasktype ='D') or  (tasktype ='O' and runtime>sysdate()) ");
				QueueListener.list = query.list();
				QuratzJobDataMgr mgr = new QuratzJobDataMgr();
				QueueListener.qa_projlist = session.createQuery("from SectorProjects where projecttype=0 order by projectid asc").list();
				//将任务加入到缓存中
				for (TestJobs tb : QueueListener.list) {
					try{
						mgr.addRunTime(tb, tb.getId());
					}catch(Exception e){
						log.error("启动 定时任务失败：" + e.getMessage());
						System.out.println("任务ID号"+tb.getId()+"启动失败，请检查任务配置情况，尤其是Cron表达式是否正确！！！！");
						continue;
					}					
				}
				if (QueueListener.list.size() != 0) {
				//设置任务的状态为启动
					session.beginTransaction();
					TestJobs tjob = new TestJobs();
					tjob.setState("1");
					Query query2 = session.createQuery("update TestJobs t set t.state = '1'  where (tasktype ='D') or  (tasktype ='O' and runtime>sysdate() ) ");
					query2.executeUpdate();
					session.getTransaction().commit();
				}
				isRun = true;

			} catch (Exception e) {
				log.error("启动 定时任务失败：" + e.getMessage());
				System.out.println("启动 定时任务失败：" + e.getMessage());
				throw new Exception("启动 定时任务失败：" + e);
			}finally{
				session.close();
			}
		}
	}

	/**
	 * 关闭 定时任务，修改状态为未启动 state=0
	 * 
	 * @throws Exception
	 */
	public static void stop(Session session) throws Exception {
		// QuratzJobDataMgr query = QuratzJobDataMgr.getInstance();
		try {
			// Session session=sessionFactory.openSession();
			session.beginTransaction();
			Query query = session.createQuery("update TestJobs t set t.state = '0'  where (tasktype ='D') or  (tasktype ='O' and to_date(runtime,'yyyy-MM-dd HH24:mi:ss')>sysdate ) ");
			query.executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			throw new Exception("关闭 定时任务失败：" + e);
		}finally{
			session.close();
		}
	}

	@Override
	public int add(TestJobs tjob) throws Exception{
		this.getHibernateTemplate().save(tjob);
		return tjob.getId();
	}

	@Override
	public void modify(TestJobs tjob) throws Exception{
		this.getHibernateTemplate().update(tjob);
		//Query query = this.getSession().createQuery("update TestJobs t set t.projPath =?,t.startDate=?,t.startTime=?,t.endDate=?,t.end where id =? ");
		//query.executeUpdate();

	}

	@Override
	public void modifyState(TestJobs tjob) throws Exception{
		this.getHibernateTemplate().update(tjob);
	}

	@Override
	public void delete(int id) throws Exception{
		try{
		TestJobs tjob = this.load(id);
		this.getHibernateTemplate().delete(tjob);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

//	public List<TestJobs> list(TestJobs tjob) {
//		Session session = this.getSession();
//		String hql = "from TestJobs ";
//		hql += where(tjob);
//		Query query = session.createQuery(hql);
//		whereParameter(tjob, query);
//		List<TestJobs> list = query.list();
//		return list;
//	}
	
	
	@Override
	public List<TestJobs> list(TestJobs tjob) {
		@SuppressWarnings("unchecked")
		List<TestJobs> list = this.getHibernateTemplate().find(" from TestJobs");
		return list;
	}

	@Override
	public List<TestJobs> list() {
		return this.getHibernateTemplate().loadAll(TestJobs.class);
	}

	@Override
	public TestJobs load(int id) {
		return (TestJobs) this.getHibernateTemplate().get(TestJobs.class, id);
	}

	/*public List<TestJobs> listFenye(TestJobs tjob, int curpage, int pagesize) {
		Session session = this.getSession();
		String hql = "from TestJobs ";
		hql += where(tjob);
		// Query query=session.createQuery(hql);
		List<TestJobs> list = findOnePage(session, hql, tjob, curpage, pagesize);
		return list;
	}

	public List<TestJobs> findOnePage(Session session, String strHQL,
			TestJobs tjob, int offset, int pagesize) {
		List<TestJobs> lst = null;
		try {
			Query query = session.createQuery(strHQL);
			whereParameter(tjob, query);
			if (offset != 0 && pagesize != 0) {
				query.setFirstResult((offset - 1) * pagesize);
				query.setMaxResults(pagesize);
			}
			lst = query.list();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return lst;
	}*/
	
	
	private void whereParameter(TestJobs jb, Query query) {
		if (!StrLib.isEmpty(jb.getTaskName())) {
			query.setParameter("name", jb.getTaskName().trim());
		}
		if (!StrLib.isEmpty(jb.getTaskType())) {
			query.setParameter("taskType", jb.getTaskType().trim());
		}
		if (!StrLib.isEmpty(jb.getPlanproj())) {
			query.setParameter("planproj", jb.getPlanproj().trim());
		}

	}

	@Override
	public TestJobs get(int id) {
		return (TestJobs) this.getHibernateTemplate().get(TestJobs.class, id);
	}

	@Override
	public List<TestJobs> load(String name, String cmdType, String planPath) {
		Session session=this.getSession(true);
		List<TestJobs> list=null;
		try{
		String hql = " from TestJobs where planName=? and cmdType=? and planPath=?";
		list= (List<TestJobs>)session
				.createQuery(hql).setString(0, name).setString(1, cmdType)
				.setString(2, planPath).list();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			session.close();
		}
		return list;
	}

	
	@Override
	/**
	 * 修改信息
	 */
	public void modifyInfo(TestJobs tjob) {
		Session session=this.getSession(true);
		try {
			session	.createQuery(
					"update TestJobs set startDate=? ,startTime=?, endDate=?, endTime=?,startTimestr=?,runTime=? where id=? ")
			.executeUpdate();
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			session.close();
		}
	}

	
	/**
	 * 使用hql语句进行分页查询
	 * 
	 * @param hql
	 *            需要查询的hql语句
	 * @param value
	 *            如果hql 有一个参数需要传入，value就是传入Hql语句的参数
	 * 
	 * @param offset
	 *            第一条记录索引
	 * 
	 * @param pageSize
	 *            每页需要显示的记录条数
	 * @return 当前页的所有记录
	 */
	public List findByPage(final String hql, final Object value,
			final int offset, final int pageSize) {
		// 通过一个HibernateCallback 对象来执行查询
		//System.out.println(hql);
		List list = getHibernateTemplate().executeFind(new HibernateCallback() {
			// 实现hibernateCallback接口必须实现的方法
			
			public Object doInHibernate(Session session)
					throws HibernateException {
				// 执行hibernate 分页查询
				Query query= session.createQuery(hql);
				whereParameter((TestJobs)value, query);
				List result =query
						.setFirstResult(offset).setMaxResults(pageSize).list();
				session.close();
				return result;
			}

		});
		return list;

	}

	

	public int findRows(TestJobs jobs,String hql) {
		int s=0;
		Session session=this.getSession(true);
		try {
			Query query=session.createQuery(hql);
			whereParameter(jobs, query);
			s= Integer.valueOf(
					query
					.list().get(0)
					.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			session.close();
		}
		return s;
		}

	public List<TestJobs> getListForPage(final String hql, final int offset,
			final int lengh) {
		log.debug("finding ListForPage");
		try {
			List list = getHibernateTemplate().executeFind(
					new HibernateCallback() {
						public Object doInHibernate(Session session)
								throws HibernateException, SQLException {
							List list2 = session.createQuery(hql)
									.setFirstResult(offset)
									.setMaxResults(lengh).list();
							session.close();
							return list2;
						}
					});
			return list;
		} catch (RuntimeException re) {
			log.error("find ListForPage failed", re);
			throw re;
		}
	}

	@Override
	public List findJobsList(String hql) {
//		List<TestTastexcute> list=new ArrayList<TestTastexcute>();
		Session session = this.getSession(true);
		List count=null;
		try {
			count = session.createSQLQuery(hql).list();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			session.close();
		}
		return count;
		// return this.getHibernateTemplate().find("select id,name,planproj from TestJobs  order by id asc ");
	}

}