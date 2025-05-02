import java.sql.*;
import java.util.*;

public class AuthService {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/ShopGiay?useSSL=false&serverTimezone=UTC";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "123456";
    private Connection connection;
    private String currentUsername;

    public AuthService() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            createTablesIfNotExists();
            createAdminIfNotExists();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 // Khiem commit
    private void createTablesIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(50) PRIMARY KEY," +
                    "password VARCHAR(50) NOT NULL," +
                    "name VARCHAR(100) NOT NULL," +
                    "address VARCHAR(255)," +
                    "phone VARCHAR(20)," +
                    "is_admin BOOLEAN)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS products (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "stock INT NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50)," +
                    "total DOUBLE," +
                    "FOREIGN KEY (username) REFERENCES users(username))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS order_items (" +
                    "order_id INT," +
                    "product_id INT," +
                    "quantity INT," +
                    "price DOUBLE," +
                    "PRIMARY KEY (order_id, product_id)," +
                    "FOREIGN KEY (order_id) REFERENCES orders(id)," +
                    "FOREIGN KEY (product_id) REFERENCES products(id))");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createAdminIfNotExists() {
        String query = "INSERT IGNORE INTO users (username, password, name, address, phone, is_admin) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "admin");
            pstmt.setString(2, "admin");
            pstmt.setString(3, "Admin");
            pstmt.setString(4, "System");
            pstmt.setString(5, "0000000000");
            pstmt.setBoolean(6, true);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void register(String username, String password, String name, String address, String phone) {
        String query = "INSERT INTO users (username, password, name, address, phone, is_admin) VALUES (?, ?, ?, ?, ?, false)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, name);
            stmt.setString(4, address);
            stmt.setString(5, phone);
            stmt.executeUpdate();
            System.out.println("Dang ky thanh cong!");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Ten dang nhap da ton tai!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean login(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                currentUsername = username;
                boolean isAdmin = rs.getBoolean("is_admin");
                System.out.println("Dang nhap thanh cong! Vai tro: " + (isAdmin ? "Quan tri vien" : "Khach hang"));
                return isAdmin;
            } else {
                System.out.println("Ten dang nhap hoac mat khau khong dung");
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static class UserService {
        private Connection connection;

        public UserService(Connection connection) {
            this.connection = connection;
        }

        public void manageUsers() {
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== QUAN LY NGUOI DUNG ===");
                System.out.println("1. Danh sach nguoi dung");
                System.out.println("2. Tim kiem nguoi dung");
                System.out.println("3. Xoa nguoi dung");
                System.out.println("4. Cap nhat thong tin");
                System.out.println("0. Quay lai");
                System.out.print("Chon: ");
                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        listUsers();
                        break;
                    case 2:
                        System.out.print("Nhap tu khoa: ");
                        searchUser(sc.nextLine());
                        break;
                    case 3:
                        System.out.print("Nhap ten dang nhap can xoa: ");
                        deleteUser(sc.nextLine());
                        break;
                    case 4:
                        System.out.print("Nhap ten dang nhap can cap nhat: ");
                        String username = sc.nextLine();
                        System.out.print("Ten moi: ");
                        String name = sc.nextLine();
                        System.out.print("Dia chi moi: ");
                        String address = sc.nextLine();
                        System.out.print("So dien thoai moi: ");
                        String phone = sc.nextLine();
                        updateUser(username, name, address, phone);
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("Lua chon khong hop le!");
                }
            }
        }

        private void listUsers() {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                System.out.println("\n--- DANH SACH NGUOI DUNG ---");
                while (rs.next()) {
                    System.out.printf("Ten dang nhap: %s | Ten: %s | Vai tro: %s%n",
                            rs.getString("username"),
                            rs.getString("name"),
                            rs.getBoolean("is_admin") ? "Admin" : "User");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void searchUser(String keyword) {
            String query = "SELECT * FROM users WHERE name LIKE ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, "%" + keyword + "%");
                ResultSet rs = stmt.executeQuery();
                System.out.println("\n--- KET QUA TIM KIEM ---");
                while (rs.next()) {
                    System.out.printf("Ten dang nhap: %s | Ten: %s%n",
                            rs.getString("username"),
                            rs.getString("name"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void deleteUser(String username) {
            String query = "DELETE FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "Xoa thanh cong!" : "Khong tim thay nguoi dung!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void updateUser(String username, String name, String address, String phone) {
            String query = "UPDATE users SET name = ?, address = ?, phone = ? WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setString(2, address);
                stmt.setString(3, phone);
                stmt.setString(4, username);
                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "Cap nhat thanh cong!" : "Cap nhat that bai!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class ProductService {
        private Connection connection;

        public ProductService(Connection connection) {
            this.connection = connection;
        }

        public void manageProducts() {
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== QUAN LY SAN PHAM ===");
                System.out.println("1. Danh sach san pham");
                System.out.println("2. Them san pham");
                System.out.println("3. Tim kiem san pham");
                System.out.println("4. Cap nhat san pham");
                System.out.println("5. Xoa san pham");
                System.out.println("0. Quay lai");
                System.out.print("Chon: ");
                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        listProducts();
                        break;
                    case 2:
                        addProduct(sc);
                        break;
                    case 3:
                        System.out.print("Nhap tu khoa: ");
                        searchProduct(sc.nextLine());
                        break;
                    case 4:
                        updateProduct(sc);
                        break;
                    case 5:
                        System.out.print("Nhap ID san pham can xoa: ");
                        deleteProduct(sc.nextInt());
                        sc.nextLine();
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("Lua chon khong hop le!");
                }
            }
        }

        private void listProducts() {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
                System.out.println("\n--- DANH SACH SAN PHAM ---");
                while (rs.next()) {
                    System.out.printf("ID: %d | Ten: %-20s | Gia: %,.2f | Ton kho: %d%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("stock"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void addProduct(Scanner sc) {
            System.out.print("Ten san pham: ");
            String name = sc.nextLine();
            System.out.print("Gia: ");
            double price = sc.nextDouble();
            System.out.print("So luong ton kho: ");
            int stock = sc.nextInt();
            sc.nextLine();

            String query = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setDouble(2, price);
                stmt.setInt(3, stock);
                stmt.executeUpdate();
                System.out.println("Them san pham thanh cong!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void searchProduct(String keyword) {
            String query = "SELECT * FROM products WHERE name LIKE ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, "%" + keyword + "%");
                ResultSet rs = stmt.executeQuery();
                System.out.println("\n--- KET QUA TIM KIEM ---");
                while (rs.next()) {
                    System.out.printf("ID: %d | Ten: %-20s | Gia: %,.2f%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void updateProduct(Scanner sc) {
            System.out.print("Nhap ID san pham can cap nhat: ");
            int id = sc.nextInt();
            sc.nextLine();
            System.out.print("Ten moi: ");
            String name = sc.nextLine();
            System.out.print("Gia moi: ");
            double price = sc.nextDouble();
            System.out.print("Ton kho moi: ");
            int stock = sc.nextInt();
            sc.nextLine();

            String query = "UPDATE products SET name = ?, price = ?, stock = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setDouble(2, price);
                stmt.setInt(3, stock);
                stmt.setInt(4, id);
                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "Cap nhat thanh cong!" : "Khong tim thay san pham!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void deleteProduct(int productId) {
            String query = "DELETE FROM products WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, productId);
                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "Xoa thanh cong!" : "Khong tim thay san pham!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class CustomerService {
        private Connection connection;
        private String username;
        private OrderService orderService;

        public CustomerService(Connection connection, String username) {
            this.connection = connection;
            this.username = username;
            this.orderService = new OrderService(connection, username);
        }

        public void showCustomerMenu() {
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== MENU KHACH HANG ===");
                System.out.println("1. Duyet san pham");
                System.out.println("2. Tim kiem san pham");
                System.out.println("3. Xem chi tiet san pham");
                System.out.println("4. Quan ly gio hang");
                System.out.println("5. Lich su don hang");
                System.out.println("6. Cai dat tai khoan");
                System.out.println("0. Dang xuat");
                System.out.print("Chon: ");

                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        orderService.browseProducts();
                        break;
                    case 2:
                        System.out.print("Nhap tu khoa: ");
                        orderService.searchProducts(sc.nextLine());
                        break;
                    case 3:
                        System.out.print("Nhap ID san pham: ");
                        orderService.viewProductDetails(sc.nextInt());
                        sc.nextLine();
                        break;
                    case 4:
                        orderService.manageCart();
                        break;
                    case 5:
                        orderService.viewOrderHistory();
                        break;
                    case 6:
                        manageAccount(sc);
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("Lua chon khong hop le!");
                }
            }
        }

        private void manageAccount(Scanner sc) {
            System.out.println("\n=== CAI DAT TAI KHOAN ===");
            System.out.println("1. Doi mat khau");
            System.out.println("2. Cap nhat thong tin");
            System.out.println("0. Quay lai");
            System.out.print("Chon: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Mat khau moi: ");
                    updatePassword(sc.nextLine());
                    break;
                case 2:
                    System.out.print("Ten moi: ");
                    String name = sc.nextLine();
                    System.out.print("Dia chi moi: ");
                    String address = sc.nextLine();
                    System.out.print("So dien thoai moi: ");
                    String phone = sc.nextLine();
                    updateInfo(name, address, phone);
                    break;
            }
        }

        private void updatePassword(String newPass) {
            String query = "UPDATE users SET password = ? WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, newPass);
                stmt.setString(2, username);
                stmt.executeUpdate();
                System.out.println("Cap nhat mat khau thanh cong!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void updateInfo(String name, String address, String phone) {
            String query = "UPDATE users SET name = ?, address = ?, phone = ? WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setString(2, address);
                stmt.setString(3, phone);
                stmt.setString(4, username);
                stmt.executeUpdate();
                System.out.println("Cap nhat thong tin thanh cong!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class OrderService {
        private Connection connection;
        private String username;
        private Map<Integer, Integer> cart = new HashMap<>();

        public OrderService(Connection connection, String username) {
            this.connection = connection;
            this.username = username;
        }

        public void browseProducts() {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
                System.out.println("\n--- DANH SACH SAN PHAM ---");
                while (rs.next()) {
                    System.out.printf("ID: %d | Ten: %-20s | Gia: %,.2f | Ton kho: %d%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("stock"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void searchProducts(String keyword) {
            String query = "SELECT * FROM products WHERE name LIKE ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, "%" + keyword + "%");
                ResultSet rs = stmt.executeQuery();
                System.out.println("\n--- KET QUA TIM KIEM ---");
                while (rs.next()) {
                    System.out.printf("ID: %d | Ten: %-20s | Gia: %,.2f%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void viewProductDetails(int productId) {
            String query = "SELECT * FROM products WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, productId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("\n--- CHI TIET SAN PHAM ---");
                    System.out.println("ID: " + rs.getInt("id"));
                    System.out.println("Ten: " + rs.getString("name"));
                    System.out.printf("Gia: %,.2f%n", rs.getDouble("price"));
                    System.out.println("Ton kho: " + rs.getInt("stock"));
                } else {
                    System.out.println("Khong tim thay san pham!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void manageCart() {
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== QUAN LY GIO HANG ===");
                System.out.println("1. Xem gio hang");
                System.out.println("2. Them san pham");
                System.out.println("3. Sua so luong");
                System.out.println("4. Xoa san pham");
                System.out.println("5. Thanh toan");
                System.out.println("0. Quay lai");
                System.out.print("Chon: ");
                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        viewCart();
                        break;
                    case 2:
                        System.out.print("Nhap ID san pham: ");
                        int pid = sc.nextInt();
                        System.out.print("So luong: ");
                        addToCart(pid, sc.nextInt());
                        sc.nextLine();
                        break;
                    case 3:
                        System.out.print("Nhap ID san pham: ");
                        int upid = sc.nextInt();
                        System.out.print("So luong moi: ");
                        updateCartItem(upid, sc.nextInt());
                        sc.nextLine();
                        break;
                    case 4:
                        System.out.print("Nhap ID san pham: ");
                        removeFromCart(sc.nextInt());
                        sc.nextLine();
                        break;
                    case 5:
                        checkout();
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("Lua chon khong hop le!");
                }
            }
        }

        private void addToCart(int productId, int quantity) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT stock FROM products WHERE id = ?");
                ps.setInt(1, productId);
                ResultSet rs = ps.executeQuery();
                
                if (rs.next() && rs.getInt("stock") >= quantity) {
                    cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
                    System.out.println("Them vao gio hang thanh cong!");
                } else {
                    System.out.println("So luong khong du hoac san pham khong ton tai!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void updateCartItem(int productId, int newQuantity) {
            if (cart.containsKey(productId)) {
                if (newQuantity <= 0) {
                    cart.remove(productId);
                } else {
                    cart.put(productId, newQuantity);
                }
                System.out.println("Cap nhat gio hang thanh cong!");
            } else {
                System.out.println("San pham khong co trong gio hang!");
            }
        }

        private void removeFromCart(int productId) {
            if (cart.remove(productId) != null) {
                System.out.println("Xoa san pham khoi gio hang!");
            } else {
                System.out.println("San pham khong co trong gio hang!");
            }
        }

        public void viewCart() {
            if (cart.isEmpty()) {
                System.out.println("\nGio hang trong!");
                return;
            }

            double total = 0;
            System.out.println("\n--- GIO HANG CUA BAN ---");
            try {
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    int productId = entry.getKey();
                    int quantity = entry.getValue();
                    
                    PreparedStatement ps = connection.prepareStatement("SELECT name, price FROM products WHERE id = ?");
                    ps.setInt(1, productId);
                    ResultSet rs = ps.executeQuery();
                    
                    if (rs.next()) {
                        double price = rs.getDouble("price");
                        total += price * quantity;
                        System.out.printf("ID: %d | Ten: %-20s | So luong: %d | Thanh tien: %,.2f%n",
                                productId, rs.getString("name"), quantity, price * quantity);
                    }
                }
                System.out.printf("TONG CONG: %,.2f%n", total);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void checkout() {
            if (cart.isEmpty()) {
                System.out.println("Gio hang trong!");
                return;
            }

            try {
                connection.setAutoCommit(false);

                // Calculate total
                double total = 0;
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    int productId = entry.getKey();
                    int quantity = entry.getValue();
                    PreparedStatement ps = connection.prepareStatement("SELECT price FROM products WHERE id = ?");
                    ps.setInt(1, productId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        total += rs.getDouble("price") * quantity;
                    }
                }

                // Create order
                String orderQuery = "INSERT INTO orders (username, total) VALUES (?, ?)";
                int orderId;
                try (PreparedStatement stmt = connection.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, username);
                    stmt.setDouble(2, total);
                    stmt.executeUpdate();
                    ResultSet rs = stmt.getGeneratedKeys();
                    rs.next();
                    orderId = rs.getInt(1);
                }

                // Add order items
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    int productId = entry.getKey();
                    int quantity = entry.getValue();

                    // Check stock
                    PreparedStatement ps = connection.prepareStatement("SELECT stock FROM products WHERE id = ?");
                    ps.setInt(1, productId);
                    ResultSet rs = ps.executeQuery();
                    
                    if (rs.next() && rs.getInt("stock") >= quantity) {
                        // Add to order_items
                        String itemQuery = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement itemStmt = connection.prepareStatement(itemQuery)) {
                            itemStmt.setInt(1, orderId);
                            itemStmt.setInt(2, productId);
                            itemStmt.setInt(3, quantity);
                            
                            PreparedStatement priceStmt = connection.prepareStatement("SELECT price FROM products WHERE id = ?");
                            priceStmt.setInt(1, productId);
                            ResultSet priceRs = priceStmt.executeQuery();
                            priceRs.next();
                            itemStmt.setDouble(4, priceRs.getDouble("price"));
                            
                            itemStmt.executeUpdate();
                        }

                        // Update stock
                        String updateStock = "UPDATE products SET stock = stock - ? WHERE id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateStock)) {
                            updateStmt.setInt(1, quantity);
                            updateStmt.setInt(2, productId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        throw new SQLException("Khong du ton kho cho san pham ID: " + productId);
                    }
                }

                connection.commit();
                cart.clear();
                System.out.println("Thanh toan thanh cong! Don hang: " + orderId);
            } catch (SQLException e) {
                try {
                    connection.rollback();
                    System.out.println("Thanh toan that bai: " + e.getMessage());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        public void viewOrderHistory() {
            String query = "SELECT o.id, o.total, o.username FROM orders o WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                System.out.println("\n--- LICH SU DON HANG ---");
                while (rs.next()) {
                    System.out.printf("Don hang #%d | Tong tien: %,.2f%n",
                            rs.getInt("id"),
                            rs.getDouble("total"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AuthService authService = new AuthService();

        while (true) {
            System.out.println("\n=== CHAO MUNG DEN VOI SHOP GIAY ===");
            System.out.println("1. Dang ky");
            System.out.println("2. Dang nhap");
            System.out.println("0. Thoat");
            System.out.print("Chon: ");
            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == 1) {
                System.out.print("Ten dang nhap: ");
                String username = sc.nextLine();
                System.out.print("Mat khau: ");
                String password = sc.nextLine();
                System.out.print("Ho ten: ");
                String name = sc.nextLine();
                System.out.print("Dia chi: ");
                String address = sc.nextLine();
                System.out.print("So dien thoai: ");
                String phone = sc.nextLine();
                authService.register(username, password, name, address, phone);
            } else if (choice == 2) {
                System.out.print("Ten dang nhap: ");
                String username = sc.nextLine();
                System.out.print("Mat khau: ");
                String password = sc.nextLine();

                Boolean isAdmin = authService.login(username, password);
                if (isAdmin != null) {
                    if (isAdmin) {
                        UserService userService = new UserService(authService.connection);
                        ProductService productService = new ProductService(authService.connection);
                        while (true) {
                            System.out.println("\n=== MENU QUAN TRI ===");
                            System.out.println("1. Quan ly nguoi dung");
                            System.out.println("2. Quan ly san pham");
                            System.out.println("0. Dang xuat");
                            System.out.print("Chon: ");
                            int adminChoice = sc.nextInt();
                            sc.nextLine();

                            switch (adminChoice) {
                                case 1:
                                    userService.manageUsers();
                                    break;
                                case 2:
                                    productService.manageProducts();
                                    break;
                                case 0:
                                    break;
                                default:
                                    System.out.println("Lua chon khong hop le!");
                            }
                            if (adminChoice == 0) break;
                        }
                    } else {
                        new CustomerService(authService.connection, username).showCustomerMenu();
                    }
                }
            } else if (choice == 0) {
                break;
            }
        }

        authService.closeConnection();
        sc.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}