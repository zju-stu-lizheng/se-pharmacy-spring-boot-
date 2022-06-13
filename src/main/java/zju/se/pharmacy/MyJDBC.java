package zju.se.pharmacy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

public class MyJDBC {

    /**
     * 登录数据库所需的信息:包括驱动器，数据库名称以及登录名、密码
     */
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    // remote database
    static final String DB_URL = "jdbc:mysql://124.220.171.17:3306/pharmacy?useSSL=false&serverTimezone=UTC";
    static final String USERNAME = "Pharmacy";
    static final String PASSWD = "lizheng";
    // 每页的药品数量
    static final int DRUGS_PER_PAGE = 50;

    // sql
    static Connection connection = null;
    static String anoString = "001";
    static Boolean isAdministrator;

    // 日期格式
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    // 处理字符串转义
    static String execString(String function) {
        StringBuffer _function = new StringBuffer();
        int j = 0;
        if (function != null) {
            for (j = 0; j < function.length(); j++) {
                if (function.charAt(j) == '"') {
                    _function.append("\\\"");
                } else if (function.charAt(j) == '\\') {
                    _function.append("\\\\");
                } else {
                    _function.append(function.charAt(j));
                }
            }
        }
        return _function.toString();
    }

    /**
     * 构造函数，为管理员新建一个对象
     * 
     * @param ano : 管理员 id
     */
    public MyJDBC(String ano) {
        isAdministrator = true;
        anoString = ano;
        connectDatabase();
    }

    /**
     * 构造函数，为用户新建一个对象
     */
    public MyJDBC() {
        isAdministrator = false;
        connectDatabase();
    }

    /**
     * 删除表格内容，方便下次进行测试
     * 
     * @param tableString : 表名
     */
    public static void doDeleteTable(String tableString) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM " + tableString + ";");
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * 连接JDBC数据库
     */
    public static void connectDatabase() {
        // 加载驱动程序
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }

        // 获取与数据库连接的对象-Connetcion
        try {
            // 如果是关闭状态，则打开
            if (connection == null)
                connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWD);
            if (!connection.isClosed())
                System.out.println("Succeeded connecting to the Database!");
            // Set auto commit as false.
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开数据库连接
     */
    public static void disconnectDatabase() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 确认管理员登录的账户密码是否正确
     * 
     * @param ano    : 账号id
     * @param passwd : 账号密码
     * @return :true of false
     */
    public static boolean ensureLogin(String ano, String passwd) {
        String pwd = null;
        ResultSet resultSet;
        PreparedStatement ps = null;

        try {
            // 创建PreparedStatement
            ps = connection.prepareStatement("SELECT password FROM administrator Where ano = ?;");
            ps.setString(1, ano); // 给第一个问号赋值
            // 执行sql语句,拿到结果集
            resultSet = ps.executeQuery();
            // 遍历结果集，得到数据
            if (resultSet.next()) {
                pwd = resultSet.getString(1);
            } else {
                System.out.println("无该用户:" + ano);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (passwd.equals(pwd)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 插入一条管理员信息
     * 
     * @param ano         : 管理员 id
     * @param aname       : 管理员 昵称
     * @param password    : 管理员 密码
     * @param phonenumber : 管理员 联系方式
     * @return : true(插入成功)/false(插入失败)
     */
    public static boolean insertAdministator(String ano, String aname, String password, String phonenumber) {
        PreparedStatement ps = null;
        try {
            // 创建PreparedStatement
            ps = connection.prepareStatement("INSERT INTO administrator VALUES(?,?,?,?);");
            ps.setString(1, ano); // 给第一个问号赋值
            ps.setString(2, aname);
            ps.setString(3, password);
            ps.setString(4, phonenumber);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取所有的药品id
     * 
     * @return Vector
     */
    public static Vector<String> getAllMedicineID() {
        Vector<String> allID = new Vector<String>();
        Statement statement;
        ResultSet resultSet;
        String sqlExecutionString = "";
        try {
            statement = connection.createStatement();
            // 执行sql语句,拿到结果集
            sqlExecutionString = String.format("SELECT id FROM db_drugs;");
            resultSet = statement.executeQuery(sqlExecutionString);
            // 遍历结果集，得到数据
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                allID.add(id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allID;
    }

    /**
     * 药品入库(单种)
     * 
     * @param id             : 药品 id
     * @param effective_date : 药品 有效日期 <YYYY-MM-DD>
     * @param storehouse_id  : 库房 id <char(255)>
     * @param stock          : 药品 库存(入库数量)
     * @return : true(插入成功)/false(插入失败)
     */
    public static boolean insertMedicine(String id, String effective_date, String storehouse_id, int stock) {
        PreparedStatement ps = null;
        try {
            // insert into database
            // 创建PreparedStatement
            ps = connection.prepareStatement("INSERT INTO medicine VALUES(?,?,?,?);");
            ps.setString(1, id); // 给第一个问号赋值
            ps.setString(2, effective_date);
            ps.setString(3, storehouse_id);
            ps.setInt(4, stock);
            ps.executeUpdate();
            // add a log
            String option = "insert medicine";
            ps = connection.prepareStatement("INSERT INTO log VALUES(?,?,?,?,?,?);");
            ps.setString(1, anoString); // 给第一个问号赋值
            ps.setString(2, option);
            ps.setString(3, id);
            ps.setString(4, effective_date);
            ps.setString(5, storehouse_id);
            ps.setInt(6, stock);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除一条药品信息(如不存在，则返回false)
     * 
     * @param id             : 药品 id
     * @param storehouse_id  : 库房 id <char(2)>
     * @param effective_date : 药品 有效日期 <YYYY-MM-DD>
     * @param stock          : 药品 库存(入库数量)
     * @return : true(删除成功)/false(删除失败)
     */
    public static boolean deleteMedicine(String id, String storehouse_id, String effective_date) {
        ResultSet resultSet;
        PreparedStatement ps = null;
        try {
            // 创建PreparedStatement
            ps = connection.prepareStatement("SELECT stock FROM medicine WHERE id = ?;");
            ps.setString(1, id); // 给第一个问号赋值
            // 查询该药品的库存
            resultSet = ps.executeQuery();
            // 判断是否存在该药品，若不存在返回false
            if (!resultSet.next())
                return false;
            // 记录该药品下架前的库存
            int stock = Integer.valueOf(resultSet.getString(1));
            // 1. 删除该药品的记录
            ps = connection
                    .prepareStatement("DELETE FROM medicine WHERE id=? AND storehouse_id=? AND effective_date=?;");
            ps.setString(1, id); // 给第一个问号赋值
            ps.setString(2, storehouse_id);
            ps.setString(3, effective_date);
            ps.executeUpdate();
            // 2. 添加到日志
            String option = "delete medicine";
            ps = connection.prepareStatement("INSERT INTO log VALUES(?,?,?,?,?,?);");
            ps.setString(1, anoString);
            ps.setString(2, option);
            ps.setString(3, id);
            ps.setString(4, effective_date);
            ps.setString(5, storehouse_id);
            ps.setInt(6, stock);
            ps.executeUpdate();
            // 两条语句均成功则提交
            connection.commit();
        } catch (SQLException e1) {
            e1.printStackTrace();
            // 否则事务回滚
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    /**
     * 药品出库(如不存在，则返回false)
     * 
     * @param id             : 药品 id
     * @param storehouse_id  : 库房 id <char(2)>
     * @param effective_date : 药品 有效日期 <YYYY-MM-DD>
     * @param num            : 药品 出库数量
     * @return : true(出库成功)/false(出库失败)
     */
    public static boolean deliveryMedicine(String id, String storehouse_id, String effective_date, int num)
            throws SQLException {
        ResultSet resultSet;
        PreparedStatement ps = null;
        try {
            // 创建PreparedStatement
            ps = connection.prepareStatement(
                    "SELECT stock FROM medicine where id = ? and effective_date = ? and storehouse_id = ?;");
            ps.setString(1, id); // 给第一个问号赋值
            ps.setString(2, effective_date);
            ps.setString(3, storehouse_id);
            resultSet = ps.executeQuery();

            // 判断是否存在该药品，若不存在返回false
            if (!resultSet.next())
                return false;
            // 计算该药品出库后的库存
            int stock = Integer.valueOf(resultSet.getString(1)) - num;
            // 1. 更新该药品库存
            ps = connection.prepareStatement(
                    "UPDATE medicine set stock = ? WHERE id=? AND storehouse_id=? AND effective_date=?;");
            ps.setInt(1, stock);
            ps.setString(2, id);
            ps.setString(3, storehouse_id);
            ps.setString(4, effective_date);
            ps.executeUpdate();

            // 2. 添加到日志
            String option = "delivery medicine";
            ps = connection.prepareStatement("INSERT INTO log VALUES(?,?,?,?,?,?);");
            ps.setString(1, anoString);
            ps.setString(2, option);
            ps.setString(3, id);
            ps.setString(4, effective_date);
            ps.setString(5, storehouse_id);
            ps.setInt(6, stock);
            ps.executeUpdate();
            // 两条语句均成功则提交
            connection.commit();
        } catch (SQLException e1) {
            e1.printStackTrace();
            // 否则事务回滚
            connection.rollback();
            return false;
        }
        return true;
    }

    /**
     * 对已有药品进行增加操作
     * 
     * @param id             : 药品 id
     * @param effective_date : 药品 有效日期 <YYYY-MM-DD>
     * @param storehouse_id  : 库房 id <char(2)>
     * @param stock          : 药品 库存(入库数量)
     * @return : true(插入成功)/false(插入失败)
     */
    public static boolean addMedicine(String id, String effective_date, String storehouse_id, int stock) {
        int remainStock = 0;
        ResultSet resultSet;
        PreparedStatement ps = null;
        try {
            // 2. 添加到日志
            ps = connection.prepareStatement(
                    "SELECT stock FROM medicine where id = ? and effective_date = ? and storehouse_id = ?;");
            ps.setString(1, id);
            ps.setString(2, effective_date);
            ps.setString(3, storehouse_id);
            resultSet = ps.executeQuery();

            // 遍历结果集，得到数据
            if (resultSet.next()) {
                remainStock = Integer.valueOf(resultSet.getString(1));
                // update database
                stock += remainStock;
            } else {
                return false;
            }
            ps = connection.prepareStatement(
                    "UPDATE medicine set stock = ? where id = ? and effective_date = ? and storehouse_id = ?;");
            ps.setInt(1, stock);
            ps.setString(2, id);
            ps.setString(3, effective_date);
            ps.setString(4, storehouse_id);
            ps.executeUpdate();

            // 2. 添加到日志
            String option = "add medicine";
            ps = connection.prepareStatement("INSERT INTO log VALUES(?,?,?,?,?,?);");
            ps.setString(1, anoString);
            ps.setString(2, option);
            ps.setString(3, id);
            ps.setString(4, effective_date);
            ps.setString(5, storehouse_id);
            ps.setInt(6, stock);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 返回医院所有分部
     * 
     * @return "[\"Branch1\", \"Branch2\"]"
     */
    public static String getAllBranch() {
        // 从medicine表中获取 : storehouse_id
        String sqlQueryString = "select distinct storehouse_id from medicine;";
        Vector<String> vec = new Vector<String>();
        int i = 0;
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlQueryString);
            while (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String storehouse_id = rs.getString("storehouse_id");
                vec.add(storehouse_id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        vec.remove("玉古路店");
        vec.insertElementAt("玉古路店", 0);

        StringBuffer queryResultBuffer = new StringBuffer("[");
        for (i = 0; i < vec.size(); i++) {
            if (i == 0)
                queryResultBuffer.append("\"" + vec.get(i) + "\"");
            else {
                queryResultBuffer.append(",\"" + vec.get(i) + "\"");
            }
        }
        queryResultBuffer.append("]");

        return queryResultBuffer.toString();
    }

    /**
     * 查询指定药品id的管理端信息（保质期，药房，库存）
     *
     * @param medicineID : 药品id
     * @return : list(python)格式的药品记录
     */
    public static String searchMedicineInfo(String medicineID) {
        PreparedStatement ps = null;
        StringBuffer queryResultBuffer = new StringBuffer("[");
        int i = 0;
        String tmpString;
        try {
            ps = connection.prepareStatement("select * from medicine where id=?;");
            ps.setString(1, medicineID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String id = rs.getString("id");
                String effective_date = rs.getString("effective_date");
                String storehouse_id = rs.getString("storehouse_id");
                int stock = rs.getInt("stock");

                if (i == 0)
                    tmpString = "[\"" + id + "\",\"" + effective_date + "\",\"" + storehouse_id + "\"," + stock
                            + "]";
                else {
                    tmpString = ",[\"" + id + "\",\"" + effective_date + "\",\"" + storehouse_id + "\"," + stock
                            + "]";
                }
                /* 将每条记录添加入 buffer */
                queryResultBuffer.append(tmpString);
                i++;
            }
            queryResultBuffer.append("]");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return queryResultBuffer.toString();
    }

    /**
     * 查询指定药品id与药房id的药品记录
     *
     * @param searchContent : 药品名字
     * @param branchName    : 药房名称
     * @param pageid        : 页号:从1开始
     * @return : list(python)格式的药品记录
     */
    public static String searchMedicine(String searchContent, String branchName, int pageid) {
        int start = (pageid - 1) * DRUGS_PER_PAGE;
        searchContent = "%" + searchContent + "%";
        int numofDrugs = 0;
        PreparedStatement ps = null;

        try {
            ps = connection.prepareStatement(
                    "select count(distinct id) as cnt from medicine natural join db_drugs where name LIKE ? and storehouse_id = ?;");
            ps.setString(1, searchContent);
            ps.setString(2, branchName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                numofDrugs = rs.getInt("cnt");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        StringBuffer queryResultBuffer = new StringBuffer("{\"MediList\" : [");
        int i = 0;
        String tmpString;
        try {
            ps = connection.prepareStatement(
                    "select id,name,brand,`function`,dosage,banned,price,unit,prescription,picture,sum(stock) as allStock from medicine natural join db_drugs where name LIKE ? and storehouse_id = ? group by id order by prescription ASC limit ?,?;");
            ps.setString(1, searchContent);
            ps.setString(2, branchName);
            ps.setInt(3, start);
            ps.setInt(4, DRUGS_PER_PAGE);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String id = rs.getString("id");
                String brand = rs.getString("brand");
                String name = rs.getString("name");
                String dosage = rs.getString("dosage");
                String banned = rs.getString("banned");
                String function = rs.getString("function");
                String picture = rs.getString("picture");
                float price = rs.getFloat("price");
                int stock = rs.getInt("allStock");
                String unit = rs.getString("unit");
                int prescription = rs.getInt("prescription");
                // 处理转义
                function = execString(function);
                dosage = execString(dosage);
                banned = execString(banned);

                tmpString = "{\"ID\" : \"" + id + "\", \"Brand\" : \"" + brand + "\", \"Name\" : \"" + name
                        + "\", \"Description\" : \"" + function + "\", \"Usage\" : \"" + dosage
                        + "\", \"Taboo\" : \"" + banned + "\", \"Price\" : " + price + ", \"URL\" : \"" + picture
                        + "\", \"Num\" : " + stock + ", \"Unit\" : \"" + unit
                        + "\", \"Prescripted\" : " + prescription + "}";
                if (i != 0)
                    tmpString = "," + tmpString;
                /* 将每条记录添加入 buffer */
                queryResultBuffer.append(tmpString);
                i++;
            }
            queryResultBuffer.append("]");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int pages = numofDrugs / DRUGS_PER_PAGE;
        if (numofDrugs % DRUGS_PER_PAGE != 0) {
            pages += 1;
        }

        queryResultBuffer.append(", \"NumPages\" : " + pages + "}");
        return queryResultBuffer.toString();
    }

    /**
     * 查询指定内容的药品记录
     *
     * @param searchContent : 药品名字
     * @param pageid        : 页号:从1开始
     * @return : list(python)格式的药品记录
     */
    public static String searchMedicine(String searchContent, int pageid) {
        int start = (pageid - 1) * DRUGS_PER_PAGE;
        searchContent = "%" + searchContent + "%";
        int numofDrugs = 0;
        PreparedStatement ps = null;

        try {
            // 获取现有库存
            ps = connection.prepareStatement(
                    "select count(distinct id) as cnt from medicine natural join db_drugs where name LIKE ?;");
            ps.setString(1, searchContent);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                numofDrugs = rs.getInt("cnt");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        StringBuffer queryResultBuffer = new StringBuffer("[[");
        int i = 0;
        String tmpString;
        try (Statement stmt = connection.createStatement()) {
            ps = connection.prepareStatement(
                    "select id,name,brand,`function`,dosage,banned,price,unit,prescription,picture,sum(stock) as allStock from medicine natural join db_drugs where name LIKE ? group by id order by prescription ASC limit ?,?;");
            ps.setString(1, searchContent);
            ps.setInt(2, start);
            ps.setInt(3, DRUGS_PER_PAGE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String id = rs.getString("id");
                String brand = rs.getString("brand");
                String name = rs.getString("name");
                String dosage = rs.getString("dosage");
                String banned = rs.getString("banned");
                String function = rs.getString("function");
                String picture = rs.getString("picture");
                float price = rs.getFloat("price");
                int allStock = rs.getInt("allStock");
                String unit = rs.getString("unit");
                int prescription = rs.getInt("prescription");
                // 处理转义
                function = execString(function);
                dosage = execString(dosage);
                banned = execString(banned);

                if (i == 0)
                    tmpString = "[\"" + id + "\",\"" + brand + "\",\"" + name + "\",\"" + function + "\",\"" + dosage
                            + "\",\"" + banned + "\"," + price + ",\"" + picture + "\"," + allStock + ",\"" + unit
                            + "\"," + prescription + "]";
                else {
                    tmpString = ",[\"" + id + "\",\"" + brand + "\",\"" + name + "\",\"" + function + "\",\"" + dosage
                            + "\",\"" + banned + "\"," + price + ",\"" + picture + "\"," + allStock + ",\"" + unit
                            + "\"," + prescription + "]";
                }
                /* 将每条记录添加入 buffer */
                queryResultBuffer.append(tmpString);
                i++;
            }
            queryResultBuffer.append("]");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int pages = numofDrugs / DRUGS_PER_PAGE;
        if (numofDrugs % DRUGS_PER_PAGE != 0) {
            pages += 1;
        }

        queryResultBuffer.append("," + pages + "]");
        return queryResultBuffer.toString();
    }

    /**
     * 查询管理端的药品记录(按页展示)
     *
     * @param pageid : 页号:从1开始
     * @return : list(python)格式的药品记录
     */
    public static String queryMedicine(int pageid) {
        int start = (pageid - 1) * DRUGS_PER_PAGE;

        // 先获取满足要求的药品条数
        String sqlQueryString = String.format("select count(distinct id) as cnt from medicine;");
        int numofDrugs = 0;
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlQueryString);
            if (rs.next()) {
                numofDrugs = rs.getInt("cnt");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PreparedStatement ps = null;
        StringBuffer queryResultBuffer = new StringBuffer("[[");
        int i = 0;
        String tmpString;
        try {
            ps = connection.prepareStatement(
                    "select id,name,brand,`function`,dosage,banned,price,unit,prescription,picture,sum(stock) as allStock from medicine natural join db_drugs group by id order by prescription ASC limit ?,?;");
            ps.setInt(1, start);
            ps.setInt(2, DRUGS_PER_PAGE);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String id = rs.getString("id");
                String brand = rs.getString("brand");
                String name = rs.getString("name");
                String dosage = rs.getString("dosage");
                String banned = rs.getString("banned");
                String function = rs.getString("function");
                String picture = rs.getString("picture");
                float price = rs.getFloat("price");
                int allStock = rs.getInt("allStock");
                // 处理转义
                function = execString(function);
                dosage = execString(dosage);
                banned = execString(banned);

                if (i == 0)
                    tmpString = "[\"" + id + "\",\"" + brand + "\",\"" + name + "\",\"" + function + "\",\"" + dosage
                            + "\",\"" + banned + "\"," + price + ",\"" + picture + "\"," + allStock + "]";
                else {
                    tmpString = ",[\"" + id + "\",\"" + brand + "\",\"" + name + "\",\"" + function + "\",\"" + dosage
                            + "\",\"" + banned + "\"," + price + ",\"" + picture + "\"," + allStock + "]";
                }
                /* 将每条记录添加入 buffer */
                queryResultBuffer.append(tmpString);
                i++;
            }
            queryResultBuffer.append("]");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int pages = numofDrugs / DRUGS_PER_PAGE;
        if (numofDrugs % DRUGS_PER_PAGE != 0) {
            pages += 1;
        }

        queryResultBuffer.append("," + pages + "]");
        return queryResultBuffer.toString();
    }

    /**
     * 查询指定药品id与药房id的药品记录
     *
     * @param medicineID : 药品id
     * @param branchName : 药房名称
     * @return : list(python)格式的药品记录
     */
    public static String queryMedicine(String medicineID, String branchName) {
        PreparedStatement ps = null;
        StringBuffer queryResultBuffer = new StringBuffer("{");
        String tmpString;
        try {
            ps = connection.prepareStatement(
                    "select id,name,brand,`function`,dosage,banned,price,picture,unit,sum(stock) as allStock,unit,prescription from medicine natural join db_drugs where id=? and storehouse_id = ? group by id");
            ps.setString(1, medicineID);
            ps.setString(2, branchName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String id = rs.getString("id");
                String brand = rs.getString("brand");
                String name = rs.getString("name");
                String dosage = rs.getString("dosage");
                String banned = rs.getString("banned");
                String function = rs.getString("function");
                String picture = rs.getString("picture");
                float price = rs.getFloat("price");
                int allStock = rs.getInt("allStock");
                String unit = rs.getString("unit");
                int prescription = rs.getInt("prescription");

                function = execString(function);
                dosage = execString(dosage);
                banned = execString(banned);
                tmpString = "\"ID\" : \"" + id + "\", \"Brand\" : \"" + brand + "\", \"Name\" : \"" + name
                        + "\", \"Description\" : \"" + function + "\", \"Usage\" : \"" + dosage
                        + "\", \"Taboo\" : \"" + banned + "\", \"Price\" : " + price + ", \"URL\" : \"" + picture
                        + "\", \"Num\" : " + allStock + ", \"Unit\" : \"" + unit
                        + "\", \"Prescripted\" : " + prescription;
                /* 将每条记录添加入 buffer */
                queryResultBuffer.append(tmpString);
            }
            queryResultBuffer.append("}");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return queryResultBuffer.toString();
    }

    /**
     * 查询目标用户购物车中的药品列表
     * 
     * @param user_id     : 用户 id
     * @param branch_name : 药房 id
     * @return : list格式的药品记录
     */
    public static String getShoppingCart(String user_id, String branch_name) {
        PreparedStatement ps = null;
        String sqlQueryString = "";
        int bill_id = -1;
        int i = 0;
        StringBuffer bills = new StringBuffer("{\"BillList\" : [");
        String bill;
        try {
            // 第一步，根据目标用户和目标药房获取所有的账单号
            ps = connection.prepareStatement(
                    "select distinct bill_id from bill where user_id = ? and storehouse_id = ?;");
            ps.setString(1, user_id);
            ps.setString(2, branch_name);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                bill_id = rs.getInt("bill_id");
                // System.out.println(bill_id);
                // 第二步，针对每一个账单需要获取药品的相关信息
                String billItemInfo = getBillItems(bill_id);
                // 第三步，获取时间，排队号，柜台号
                String order_date = ""; // 获取时间
                int isPaid = 0;

                Statement stmt = connection.createStatement();
                sqlQueryString = String.format("select order_date,isPaid from bill where bill_id = %d;", bill_id);
                ResultSet billResult = stmt.executeQuery(sqlQueryString);
                if (billResult.next()) {
                    order_date = billResult.getString("order_date");
                    isPaid = billResult.getInt("isPaid");
                }
                int qid = -1, wid = -1;
                if (isPaid == 1) { // 判断ispaid,未支付返回-1
                    sqlQueryString = String.format("select qid from SE_Queue where bill_id = %d;", bill_id);
                    billResult = stmt.executeQuery(sqlQueryString);
                    if (billResult.next()) {
                        qid = billResult.getInt("qid");
                    }
                    sqlQueryString = String.format("select wid from SE_Window where bill_id = %d;", bill_id);
                    billResult = stmt.executeQuery(sqlQueryString);
                    if (billResult.next()) {
                        wid = billResult.getInt("wid");
                    }
                }
                // 第四步，组合账单对应的药品信息
                bill = "{ \"ItemList\" : " + billItemInfo + ", \"Date\" : \"" + order_date + "\", \"BillID\" : \""
                        + bill_id + "\", \"QueueID\" : " + qid + ", \"WindowID\" : " + wid + "}";
                if (i != 0)
                    bill = "," + bill;
                bills.append(bill);
                i++;
            }
            bills.append("]}");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bills.toString();
    }

    /**
     * 根据账单号 获取具体需要药品信息
     * 
     * @param bill_id : 账单号
     * @return [
     *         ["002","国药","头孢","头孢就酒，越喝越勇","一日三次","三高人群",24.0,"https://s2.loli.net/2022/05/06/.png",10,"盒",0],...
     *         ]
     *         medicine_id,brand,name,function,dosage,banned,price,picture,num,unit,isprescription
     */
    public static String getBillItems(int bill_id) {
        StringBuffer BillItemBuffer = new StringBuffer("[");
        // 从购物车中搜索得到该用户需要买的药
        String sqlQueryString = String.format("select * from bill natural join shoppingCart where bill_id = %d;",
                bill_id);
        String tmpString;
        int i = 0;
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlQueryString);
            while (rs.next()) {
                String medicine_id = rs.getString("medicine_id");
                String storehouse_id = rs.getString("storehouse_id");
                int num = rs.getInt("num");

                // 针对每一种药,从所有药品中挑选出保质期最短的药品
                String sqlQueryString2 = String.format(
                        "select * from medicine natural join db_drugs where id = '%s' AND storehouse_id = '%s' ORDER BY effective_date ASC;",
                        medicine_id, storehouse_id);
                // 执行搜索语句，我只取第一条记录
                Statement stmt2 = connection.createStatement();
                ResultSet dateResultSet = stmt2.executeQuery(sqlQueryString2);
                dateResultSet.next();
                /* 根据 属性获取该条记录相应的值 */
                String brand = dateResultSet.getString("brand");
                String name = dateResultSet.getString("name");
                String function = dateResultSet.getString("function");
                String dosage = dateResultSet.getString("dosage");
                String banned = dateResultSet.getString("banned");
                float price = dateResultSet.getFloat("price");
                String picture = dateResultSet.getString("picture");
                int isprescription = dateResultSet.getInt("prescription");
                String unit = dateResultSet.getString("unit");

                function = execString(function);
                dosage = execString(dosage);
                banned = execString(banned);

                // medicine_id,brand,name,function,dosage,banned,price,picture,num,unit,isprescription
                tmpString = "{\"ID\" : \"" + medicine_id + "\", \"Brand\" : \"" + brand + "\", \"Name\" : \"" + name
                        + "\", \"Description\" : \"" + function + "\", \"Usage\" : \"" + dosage
                        + "\", \"Taboo\" : \"" + banned + "\", \"Price\" : " + price + ", \"URL\" : \"" + picture
                        + "\", \"Num\" : " + num + ", \"Unit\" : \"" + unit
                        + "\", \"Prescripted\" : " + isprescription + "}";
                if (i != 0)
                    tmpString = "," + tmpString;
                /* 将每条记录添加入 buffer */
                BillItemBuffer.append(tmpString);
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        BillItemBuffer.append("]");
        return BillItemBuffer.toString();
    }

    /**
     * 根据有效日期和购物车中药品id 获取具体需要出库药品的信息
     * 
     * @param user_id       : 用户 id
     * @param storehouse_id : 药房 id
     * @return ArrayList<MedicineBillEntry> bill;
     */
    public static ArrayList<MedicineBillEntry> getBillEntries(String user_id, String storehouse_id) {
        ArrayList<MedicineBillEntry> list = new ArrayList<MedicineBillEntry>();
        // 从购物车中搜索得到该用户需要买的药
        String sqlQueryString = String.format(
                "select * from shoppingCart where user_id = '%s' and storehouse_id = '%s';", user_id, storehouse_id);
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlQueryString);
            while (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                String medicine_id = rs.getString("medicine_id");
                int num = rs.getInt("num");

                // 针对每一种药,从所有药品中挑选出保质期最短的药品
                String sqlQueryString2 = String.format(
                        "select brand,effective_date from medicine natural join db_drugs where id = '%s' AND storehouse_id = '%s' ORDER BY effective_date ASC;",
                        medicine_id, storehouse_id);
                // 执行搜索语句，我只取第一条记录
                Statement stmt2 = connection.createStatement();
                ResultSet dateResultSet = stmt2.executeQuery(sqlQueryString2);
                dateResultSet.next();
                /* 根据 属性获取该条记录相应的值 */
                String brand = dateResultSet.getString("brand");
                String effective_date = dateResultSet.getString("effective_date");
                MedicineBillEntry tmpBillEntry = new MedicineBillEntry(medicine_id, num, brand, storehouse_id,
                        effective_date);
                list.add(tmpBillEntry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 往购物车内插入一条药品信息 : 将数量设定成指定的数量
     * 
     * @param user_id       : 用户 id
     * @param medicine_id   : 药品 id
     * @param storehouse_id : 药房 id
     * @param num           : 数量
     * @return true(修改成功)/false(修改失败)
     * @throws SQLException
     */
    public static boolean setShoppingCart(String user_id, String medicine_id, String storehouse_id, int num)
            throws SQLException {
        PreparedStatement ps = null, ps2 = null, ps3 = null;
        ResultSet resultSet;
        Boolean hasBillBoolean;

        try {
            // 第一步:判断该用户在该药房是否有未支付的账单.如果有：获取该账单的账单号;如果无：新建一个账单，获取这个账单的账单号
            ps = connection.prepareStatement(
                    "SELECT bill_id FROM bill WHERE user_id = ? AND storehouse_id = ? AND isPaid = 0;");
            ps.setString(1, user_id);
            ps.setString(2, storehouse_id);
            resultSet = ps.executeQuery();

            int bill_id = -1;
            if (!resultSet.next()) {
                hasBillBoolean = false;
            } else {
                hasBillBoolean = true;
                bill_id = Integer.valueOf(resultSet.getString(1));
            }

            if (hasBillBoolean == false) {
                Date date = new Date();
                String order_date = formatter.format(date); // 当前日期
                ps = connection.prepareStatement(
                        "INSERT INTO bill (user_id,storehouse_id,order_date,isPaid ) VALUES(?,?,?,0);");
                ps.setString(1, user_id);
                ps.setString(2, storehouse_id);
                ps.setString(3, order_date);
                ps.executeUpdate();
                connection.commit();

                // 再次进行查询
                ps = connection.prepareStatement(
                        "SELECT bill_id FROM bill WHERE user_id = ? AND storehouse_id = ? AND isPaid = 0;");
                ps.setString(1, user_id);
                ps.setString(2, storehouse_id);
                resultSet = ps.executeQuery();
                if (!resultSet.next()) {
                    return false;
                } else {
                    bill_id = Integer.valueOf(resultSet.getString(1));
                }
            }

            // 第二步: 判断是否设置该药品数量为0
            if (num == 0) {
                // 将记录从购物车表中删除
                ps = connection.prepareStatement(
                        "DELETE FROM shoppingCart WHERE user_id=? AND medicine_id=? AND storehouse_id=? and bill_id = ?;");
                ps.setString(1, user_id);
                ps.setString(2, medicine_id);
                ps.setString(3, storehouse_id);
                ps.setInt(4, bill_id);
                ps.executeUpdate();
            } else {
                // 第三步: 往shoppingcart中插入该条数据
                ps2 = connection.prepareStatement(
                        "SELECT SUM(stock) FROM medicine WHERE id = ? AND storehouse_id = ? GROUP BY id;");
                ps2.setString(1, medicine_id);
                ps2.setString(2, storehouse_id);
                // 查询该药品的库存
                resultSet = ps2.executeQuery();
                // 判断是否存在该药品，若不存在返回false
                if (!resultSet.next()) {
                    return false;
                } else {
                    if (resultSet.getString(1) == null)
                        return false;
                }
                // 记录该药品当前的库存
                int stock = Integer.valueOf(resultSet.getString(1));
                // 查询购物车中是否已经有该药品
                ps2 = connection.prepareStatement(
                        "SELECT num FROM shoppingCart WHERE medicine_id = ? AND user_id =? AND storehouse_id = ? AND bill_id = ?;");
                ps2.setString(1, medicine_id);
                ps2.setString(2, user_id);
                ps2.setString(3, storehouse_id);
                ps2.setInt(4, bill_id);
                // 查询该药品的库存
                resultSet = ps2.executeQuery();

                // 判断是否存在该药品
                if (resultSet.next()) {
                    // 如果购物车数量大于库存，返回false
                    if (stock < num)
                        return false;
                    // 更新购物车的记录
                    ps2 = connection.prepareStatement(
                            "UPDATE shoppingCart set num = ? WHERE user_id=? AND medicine_id=? AND storehouse_id = ? AND bill_id = ?;");
                    ps2.setInt(1, num);
                    ps2.setString(2, user_id);
                    ps2.setString(3, medicine_id);
                    ps2.setString(4, storehouse_id);
                    ps2.setInt(5, bill_id);
                    ps2.executeUpdate();
                } else {
                    // 如果购物车数量大于库存，返回false
                    if (stock < num)
                        return false;
                    // 将记录插入到购物车表中
                    ps3 = connection.prepareStatement(
                            "INSERT INTO shoppingCart VALUES(?,?,?,?,?);");
                    ps3.setString(1, user_id);
                    ps3.setString(2, medicine_id);
                    ps3.setInt(3, num);
                    ps3.setString(4, storehouse_id);
                    ps3.setInt(5, bill_id);
                    ps3.executeUpdate();
                }
            }
            // 成功则提交
            connection.commit();
        } catch (SQLException e1) {
            e1.printStackTrace();
            // 失败则事务回滚
            connection.rollback();
            return false;
        }
        return true;
    }

    /**
     * 往购物车内插入一条药品信息
     * 
     * @param user_id       : 用户 id
     * @param medicine_id   : 药品 id
     * @param storehouse_id : 药房 id
     * @param num           : 数量
     * @return true(插入成功)/false(插入失败)
     * @throws SQLException
     */
    public static boolean addShoppingCart(String user_id, String medicine_id, String storehouse_id, int num)
            throws SQLException {
        String sqlExecutionString = "";
        String sqlQueryString = "";
        ResultSet resultSet;
        Statement statement;
        Boolean hasBillBoolean;

        try {
            // 获取执行sql语句的statement对象
            statement = connection.createStatement();
            // 第一步:判断该用户在该药房是否有未支付的账单.如果有：获取该账单的账单号;如果无：新建一个账单，获取这个账单的账单号
            sqlQueryString = String.format(
                    "SELECT bill_id FROM bill WHERE user_id = '%s' AND storehouse_id = '%s' AND isPaid = 0;", user_id,
                    storehouse_id);
            resultSet = statement.executeQuery(sqlQueryString);
            int bill_id = -1;
            if (!resultSet.next()) {
                hasBillBoolean = false;
            } else {
                hasBillBoolean = true;
                bill_id = Integer.valueOf(resultSet.getString(1));
            }

            if (hasBillBoolean == false) {
                Date date = new Date();
                String order_date = formatter.format(date); // 当前日期
                sqlExecutionString = String.format(
                        "INSERT INTO bill (user_id,storehouse_id,order_date,isPaid ) VALUES('%s','%s','%s',0);",
                        user_id, storehouse_id, order_date);
                // System.out.println(sqlExecutionString);
                statement.executeUpdate(sqlExecutionString);
                connection.commit();

                resultSet = statement.executeQuery(sqlQueryString);
                if (!resultSet.next()) {
                    hasBillBoolean = false;
                } else {
                    hasBillBoolean = true;
                    bill_id = Integer.valueOf(resultSet.getString(1));
                }
            }

            // 第二步: 往shoppingcart中插入该条数据
            sqlExecutionString = String.format(
                    "SELECT SUM(stock) FROM medicine WHERE id = '%s' AND storehouse_id = '%s' GROUP BY id;",
                    medicine_id, storehouse_id);
            // 查询该药品的库存
            resultSet = statement.executeQuery(sqlExecutionString);
            // 判断是否存在该药品，若不存在返回false
            if (!resultSet.next()) {
                return false;
            } else {
                if (resultSet.getString(1) == null)
                    return false;
            }
            // 记录该药品当前的库存
            int stock = Integer.valueOf(resultSet.getString(1));
            // 查询购物车中是否已经有该药品
            resultSet = statement.executeQuery(String.format(
                    "SELECT num FROM shoppingCart WHERE medicine_id = '%s' AND user_id ='%s' AND storehouse_id = '%s' AND bill_id = %d;",
                    medicine_id, user_id, storehouse_id, bill_id));
            // 判断是否存在该药品
            if (resultSet.next()) {
                num += Integer.valueOf(resultSet.getString(1));
                // 如果购物车数量大于库存，返回false
                if (stock < num)
                    return false;
                // 更新购物车的记录
                sqlExecutionString = String.format(
                        "UPDATE shoppingCart set num = %d WHERE user_id='%s' AND medicine_id='%s' AND storehouse_id = '%s' AND bill_id = %d;",
                        num, user_id, medicine_id, storehouse_id, bill_id);
                statement.executeUpdate(sqlExecutionString);
            } else {
                // 如果购物车数量大于库存，返回false
                if (stock < num)
                    return false;
                // 将记录插入到购物车表中
                sqlExecutionString = String.format("INSERT INTO shoppingCart VALUES('%s','%s', %d,'%s',%d);", user_id,
                        medicine_id, num, storehouse_id, bill_id);
                statement.executeUpdate(sqlExecutionString);
            }
            // 成功则提交
            connection.commit();
        } catch (SQLException e1) {
            e1.printStackTrace();
            // 失败则事务回滚
            connection.rollback();
            return false;
        }
        return true;
    }

    /**
     * 从购物车中减少要购买的药品的数量
     * 
     * @param user_id       : 用户 id
     * @param medicine_id   : 药品 id
     * @param storehouse_id : 药房 id
     * @param num           : 数量
     * @return true(插入成功)/false(插入失败)
     * @throws SQLException
     */
    public static boolean deleteShoppingCart(String user_id, String medicine_id, String storehouse_id, int num)
            throws SQLException {
        String sqlExecutionString = "";
        String sqlQueryString = "";
        ResultSet resultSet;
        Statement statement;
        Boolean hasBillBoolean;
        try {
            // 获取执行sql语句的statement对象
            statement = connection.createStatement();
            // 第一步:判断该用户在该药房是否有未支付的账单.如果有：获取该账单的账单号;如果无：新建一个账单，获取这个账单的账单号
            sqlQueryString = String.format(
                    "SELECT bill_id FROM bill WHERE user_id = '%s' AND storehouse_id = '%s' AND isPaid = 0;", user_id,
                    storehouse_id);
            resultSet = statement.executeQuery(sqlQueryString);
            int bill_id = -1;
            if (!resultSet.next()) {
                hasBillBoolean = false;
            } else {
                hasBillBoolean = true;
                bill_id = Integer.valueOf(resultSet.getString(1));
            }

            if (hasBillBoolean == false) {
                Date date = new Date();
                String order_date = formatter.format(date); // 当前日期
                sqlExecutionString = String.format(
                        "INSERT INTO bill (user_id,storehouse_id,order_date,isPaid ) VALUES('%s','%s','%s',0);",
                        user_id, storehouse_id, order_date);
                // System.out.println(sqlExecutionString);
                statement.executeUpdate(sqlExecutionString);
                connection.commit();

                resultSet = statement.executeQuery(sqlQueryString);
                if (!resultSet.next()) {
                    hasBillBoolean = false;
                } else {
                    hasBillBoolean = true;
                    bill_id = Integer.valueOf(resultSet.getString(1));
                }
            }

            // 第二步: 往shoppingcart中修改药品数量
            // 查询该药品的在购物车中的数量
            resultSet = statement.executeQuery(String.format(
                    "SELECT num FROM shoppingCart WHERE medicine_id = '%s' AND user_id ='%s' AND storehouse_id = '%s';",
                    medicine_id, user_id, storehouse_id));
            // 判断是否存在该药品，若不存在返回false
            if (!resultSet.next())
                return false;
            // 记录该药品在购物车当前的数量
            int oldnum = Integer.valueOf(resultSet.getString(1));
            // 如果数量小于删除量
            if (oldnum < num)
                return false;
            num = oldnum - num;
            if (num == 0) {
                // 将记录从购物车表中删除
                sqlExecutionString = String.format(
                        "DELETE FROM shoppingCart WHERE user_id='%s' AND medicine_id='%s' AND storehouse_id='%s' and bill_id = %d;",
                        user_id, medicine_id, storehouse_id, bill_id);
                statement.executeUpdate(sqlExecutionString);
            } else {
                // 将记录更新
                sqlExecutionString = String.format(
                        "UPDATE shoppingCart set num = %d WHERE user_id='%s' AND medicine_id='%s' AND storehouse_id = '%s' and bill_id = %d;",
                        num, user_id, medicine_id, storehouse_id, bill_id);
                statement.executeUpdate(sqlExecutionString);
            }
            // 成功则提交
            connection.commit();
        } catch (SQLException e1) {
            e1.printStackTrace();
            // 失败则事务回滚
            connection.rollback();
            return false;
        }
        return true;
    }

    /**
     * 用户支付完账单后，增加一个排队记录
     * 
     * @param bill_id       : 账单号
     * @param storehouse_id : 药房号
     * @return : true(插入成功)/false(插入失败)
     */
    public static boolean addQueue(int bill_id, String storehouse_id) {
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(
                    "INSERT INTO SE_Queue (bill_id,storehouse_id)" + " VALUES(" + bill_id + ",'" + storehouse_id
                            + "');");
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除排队号
     * 
     * @param bill_id : 账单号
     * @return : true(刪除成功)/false(刪除失败)
     */
    public static boolean deleteQueue(int bill_id) {
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("Delete From SE_Queue where bill_id = " + bill_id + ";");
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除窗口号
     * 
     * @param bill_id : 账单号
     * @return : true(刪除成功)/false(刪除失败)
     */
    public static boolean deleteWindow(int bill_id) {
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("Delete From SE_Window where bill_id = " + bill_id + ";");
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 调度程序调度取药窗口，将该记录存入数据库
     * 
     * @param bill_id       : 账单号
     * @param storehouse_id : 药房号
     * @param wid           : 排队号
     * @return : true(插入成功)/false(插入失败)
     */
    public static boolean addWindow(int bill_id, String storehouse_id, int wid) {
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("INSERT INTO SE_Window (bill_id,storehouse_id,wid)" + " VALUES(" + bill_id + ",'"
                    + storehouse_id + "'," + wid + ");");
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 根据窗口号返回等待的人数
     * 
     * @param storehouse_id
     * @param wid
     * @return
     */
    public static int searchWindowPeople(String storehouse_id, int wid) {
        int count = 0;
        String sqlQueryString = String.format(
                "select count(bill_id) as cnt from SE_Window where storehouse_id='%s' and wid=%d;", storehouse_id, wid);
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlQueryString);
            if (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                count = rs.getInt("cnt");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * 根据窗口号返回处于等待状态下的患者购买的药品数量
     * 
     * @param storehouse_id
     * @param wid
     * @return
     */
    public static int searchWindowMedicine(String storehouse_id, int wid) {
        int count = 0;
        String sqlQueryString = String.format(
                "select sum(num) as num_medicine from bill natural join shoppingCart natural join SE_Window where storehouse_id='%s' and wid=%d;",
                storehouse_id, wid);
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlQueryString);
            if (rs.next()) {
                /* 根据 属性获取该条记录相应的值 */
                count = rs.getInt("num_medicine");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * 用户支付订单(Bill)，药品库存需要对应减少，支付完的订单需要进行标记
     * 
     * @param user_id       : 用户 id
     * @param storehouse_id : 药房 id
     * @return 库存是否足够(true/false)
     * @throws SQLException
     */
    public static boolean commitBill(String user_id, String storehouse_id) throws SQLException {
        String sqlExecutionString = "";
        String sqlQueryString = "";
        ResultSet resultSet;
        Statement statement;
        Boolean hasBillBoolean;

        try {
            statement = connection.createStatement();
            // 1.根据 用户id和药房id 找出对应的账单
            sqlQueryString = String.format(
                    "SELECT bill_id FROM bill WHERE user_id = '%s' AND storehouse_id = '%s' AND isPaid = 0;", user_id,
                    storehouse_id);
            resultSet = statement.executeQuery(sqlQueryString);
            int bill_id = -1;
            if (!resultSet.next()) {
                hasBillBoolean = false;
            } else {
                hasBillBoolean = true;
                bill_id = Integer.valueOf(resultSet.getString(1));
            }

            if (hasBillBoolean == false) {
                // report error
                System.out.println("Error:该用户不存在未支付账单");
                return false;
            } else {
                // 2. 将该账单置为已支付,并补充支付日期
                Date date = new Date();
                String paid_date = formatter.format(date); // 当前日期
                sqlExecutionString = String.format("UPDATE bill set isPaid = 1,paid_date='%s' WHERE bill_id='%s';",
                        paid_date, bill_id);
                statement.executeUpdate(sqlExecutionString);

                // 3. 从库存中取出相应药品
                ArrayList<MedicineBillEntry> list = getBillEntries(user_id, storehouse_id);
                for (MedicineBillEntry medicineItem : list) {
                    // 取出药品 —— 判断是否有足够库存
                    resultSet = statement.executeQuery(String.format(
                            "SELECT num FROM shoppingCart WHERE medicine_id = '%s' AND user_id ='%s' AND storehouse_id = '%s';",
                            medicineItem.medicine_id, user_id, storehouse_id));
                    // 判断是否存在该药品，若不存在返回false
                    if (!resultSet.next())
                        return false;
                    // 记录该药品在购物车当前的数量
                    int oldnum = Integer.valueOf(resultSet.getString(1));
                    // 如果数量小于删除量
                    if (oldnum < medicineItem.num)
                        return false;
                    // 库存足够，出库
                    sqlExecutionString = String.format(
                            "UPDATE medicine set stock = stock - %d where id = '%s' and effective_date = '%s' and storehouse_id = '%s';",
                            medicineItem.num, medicineItem.medicine_id, medicineItem.effective_date,
                            medicineItem.storehouse_id);
                    statement.executeUpdate(sqlExecutionString);
                }
                // 4. 存入排队号与窗口号
                MyWindows.addPerson(bill_id, storehouse_id);
            }
            connection.commit();
        } catch (SQLException e1) {
            e1.printStackTrace();
            // 失败则事务回滚
            connection.rollback();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        MyJDBC.connectDatabase();
    }
}
