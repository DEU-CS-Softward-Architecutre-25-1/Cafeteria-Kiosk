package dev.qf.server.network;

import com.google.common.primitives.Ints;
import common.Order;
import common.Category;
import common.Menu;
import common.network.SynchronizeData;
import common.network.encryption.NetworkEncryptionUtils;
import common.network.packet.*;
import common.registry.Registry;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import common.network.handler.SerializableHandler;
import common.network.handler.listener.ServerPacketListener;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ServerPacketListenerImpl implements ServerPacketListener {
    private final SerializableHandler handler;
    private final Logger logger = KioskLoggerFactory.getLogger();
    private final byte[] nonce;
    private static int nextOrderId = 1; // 서버 시작 시 1로 초기화

    //백업 저장소
    private final Map<String, List<Menu>> categoryMenuBackup = new HashMap<>();

    public ServerPacketListenerImpl(SerializableHandler handler) {
        this.handler = handler;
        this.nonce = Ints.toByteArray(RandomGenerator.of("Xoroshiro128PlusPlus").nextInt());
    }

    @Override
    public void onHandShake(HandShakeC2SInfo packet) {
        handler.setId(packet.id());
        logger.info("HandShake received");
        KioskNettyServer server = (KioskNettyServer) handler.connection;
        this.handler.send(new HelloS2CPacket(server.getKeyPair().getPublic().getEncoded(), nonce));
        logger.info("Hello sent nonce : {}", nonce);
    }

    @Override
    public void onRequestData(UpdateDataPacket.RequestDataC2SPacket packet) {
        if (!this.handler.isEncrypted()) {
            throw new IllegalStateException("Client is not encrypted");
        }
        Registry<?> registry = RegistryManager.getAsId(packet.registryId());

        if (packet.registryId().equalsIgnoreCase("all")) {
            RegistryManager.entries().forEach(entry -> {
                List<SynchronizeData<?>> data = new ArrayList<>(entry.getAll());
                this.handler.send(new UpdateDataPacket.ResponseDataS2CPacket(entry.getRegistryId(), data));
            });
            return;
        }
        if (registry == null) {
            logger.warn("Registry {} not found", packet.registryId());
            logger.warn("skipping this packet...");
            return;
        }

        List<SynchronizeData<?>> data = new ArrayList<>(registry.getAll());
        UpdateDataPacket.ResponseDataS2CPacket response = new UpdateDataPacket.ResponseDataS2CPacket(packet.registryId(), data);
        this.handler.send(response);
    }

    @Override
    public void onKeyReceived(KeyC2SPacket packet) {
        try {
            KioskNettyServer server = (KioskNettyServer) handler.connection;
            PrivateKey privateKey = server.getKeyPair().getPrivate();
            if (!packet.verifySignedNonce(this.nonce, privateKey)) {
                throw new IllegalStateException("Invalid nonce");
            }

            SecretKey secretKey = packet.decryptSecretKey(privateKey);
            Cipher encryptCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secretKey);
            Cipher decryptCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.DECRYPT_MODE, secretKey);

            this.handler.encrypt(encryptCipher, decryptCipher);
            this.handler.send(new EncryptCompleteS2CPacket(System.currentTimeMillis()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(DataAddedC2SPacket packet) {
        Registry<?> registry = RegistryManager.getAsId(packet.registryId());
        registry.unfreeze();
        registry.add(packet.data().getRegistryElementId(), packet.data());
        registry.freeze();
        KioskNettyServer server = (KioskNettyServer) handler.connection;

        server.broadCast(new UpdateDataPacket.ResponseDataS2CPacket(
                registry.getRegistryId(),
                (List<SynchronizeData<?>>) registry.getAll()
        ));

    }

    @Override
    public void onDeleteReceived(DataDeletedC2SPacket packet) {
        try {
            String registryId = packet.registryId();
            String dataId = packet.dataId();

            logger.info("Processing deletion request - Registry: {}, ID: {}", registryId, dataId);

            if (RegistryManager.MENUS.getRegistryId().equals(registryId)) {
                handleMenuDeletion(dataId);
            } else if (RegistryManager.CATEGORIES.getRegistryId().equals(registryId)) {
                handleCategoryDeletion(dataId);
            } else {
                logger.warn("Deletion not supported for registry: {}", registryId);
            }

        } catch (Exception e) {
            logger.error("Failed to process deletion request", e);
        }
    }

    @Override
    public void onOrderStatusChanged(OrderStatusChangedC2SPacket packet) { // 이 메서드를 남깁니다.
        if (!this.handler.isEncrypted()) {
            throw new IllegalStateException("Client is not encrypted");
        }
        Order receivedOrder = packet.order();

        // 새 주문 ID를 nextOrderId를 이용해 부여합니다.
        int newOrderId = nextOrderId++; // nextOrderId++; 이 코드가 있는 버전을 남겨야 합니다.

        // 새로운 ID를 포함하는 Order 객체를 새로 생성합니다.
        common.Order finalOrder = new common.Order(
                newOrderId,
                receivedOrder.customer(),
                receivedOrder.orderTime(),
                common.OrderStatus.ACCEPTED, // 서버에서 상태를 PENDING->ACCEPTED로 변경할 수 있습니다.
                receivedOrder.cart()
        );

        System.out.println("--- 서버에서 수신 및 ID 부여 완료된 주문 정보 ---");
        // ... (finalOrder의 상세 정보 로그 출력) ...
        System.out.println("부여된 새 주문 ID: " + finalOrder.orderId());
        System.out.println("------------------------------------");

        try {
            RegistryManager.ORDERS.unfreeze();
            // 서버에서 새로 생성한 finalOrder 객체를 Registry에 추가합니다.
            // RegistryManager.ORDERS.addOrder(finalOrder); // OrderRegistry가 있다면 사용
            // RegistryManager.ORDERS.add(String.valueOf(finalOrder.orderId()), finalOrder); // 일반 Registry의 add 사용
            ((common.registry.OrderRegistry)RegistryManager.ORDERS).addOrder(finalOrder); // OrderRegistry로 캐스팅하여 addOrder 호출

        } finally {
            RegistryManager.ORDERS.freeze();
        }

        logger.info("서버: 주문 정보 수신 및 저장됨 - 주문 ID: {}", finalOrder.orderId());

        // 클라이언트에 새로운 주문 ID와 함께 업데이트된 Order 객체를 전송 (선택 사항)
        // KioskNettyServer server = (KioskNettyServer) handler.connection;
        // server.broadCast(new common.network.packet.OrderUpdatedS2CPacket(finalOrder));
    }

    // 백업 관련

    private void backupCurrentMenuCategoryRelations() {
        logger.info("=== Backing up current menu-category relations ===");
        categoryMenuBackup.clear();

        for (Category category : RegistryManager.CATEGORIES.getAll()) {
            List<Menu> menusCopy = new ArrayList<>(category.menus());
            categoryMenuBackup.put(category.cateId(), menusCopy);

            logger.info("Backed up category '{}' ({}) with {} menus:",
                    category.cateName(), category.cateId(), menusCopy.size());

            for (Menu menu : menusCopy) {
                logger.debug("  - {} ({})", menu.name(), menu.id());
            }
        }

        logger.info("Backup completed. Total categories backed up: {}", categoryMenuBackup.size());
        logger.info("=== Backup Summary ===");
        for (Map.Entry<String, List<Menu>> entry : categoryMenuBackup.entrySet()) {
            logger.info("Category {}: {} menus", entry.getKey(), entry.getValue().size());
        }
    }

    private void restoreOrphanMenusFromBackup() {
        if (categoryMenuBackup.isEmpty()) {
            logger.debug("No backup data available for orphan menu restoration");
            return;
        }

        logger.info("=== Checking for orphan menus ===");

        // 현재 존재하는 모든 메뉴 ID
        Set<String> currentMenuIds = RegistryManager.MENUS.getAll().stream()
                .map(Menu::id)
                .collect(Collectors.toSet());

        logger.info("Current menus in registry: {}", currentMenuIds);

        // 현재 카테고리들에 속한 메뉴 ID들
        Set<String> assignedMenuIds = RegistryManager.CATEGORIES.getAll().stream()
                .flatMap(category -> category.menus().stream())
                .map(Menu::id)
                .collect(Collectors.toSet());

        logger.info("Menus currently assigned to categories: {}", assignedMenuIds);

        Set<String> orphanMenuIds = new HashSet<>(currentMenuIds);
        orphanMenuIds.removeAll(assignedMenuIds);

        if (orphanMenuIds.isEmpty()) {
            logger.info("No orphan menus found");
            return;
        }

        logger.warn("Found {} orphan menus: {}", orphanMenuIds.size(), orphanMenuIds);

        int restoredCount = 0;
        for (String orphanMenuId : orphanMenuIds) {
            String originalCategoryId = findOriginalCategoryFromBackup(orphanMenuId);
            if (originalCategoryId != null) {
                if (restoreMenuToCategory(orphanMenuId, originalCategoryId)) {
                    restoredCount++;
                }
            } else {
                handleUnmappedOrphanMenu(orphanMenuId);
            }
        }

        logger.info("Orphan menu restoration completed. Restored: {}, Total orphans: {}",
                restoredCount, orphanMenuIds.size());
    }

    private String findOriginalCategoryFromBackup(String menuId) {
        logger.debug("Searching for original category of menu: {}", menuId);

        for (Map.Entry<String, List<Menu>> entry : categoryMenuBackup.entrySet()) {
            String categoryId = entry.getKey();
            List<Menu> menus = entry.getValue();

            boolean foundInCategory = menus.stream()
                    .anyMatch(menu -> menu.id().equals(menuId));

            if (foundInCategory) {
                logger.info("Found original category for menu '{}': '{}'", menuId, categoryId);
                return categoryId;
            }
        }

        logger.warn("Could not find original category for menu '{}' in backup", menuId);
        return null;
    }

    private boolean restoreMenuToCategory(String menuId, String categoryId) {
        Optional<Menu> menuOpt = RegistryManager.MENUS.getById(menuId);
        Optional<Category> categoryOpt = RegistryManager.CATEGORIES.getById(categoryId);

        if (menuOpt.isEmpty()) {
            logger.warn("Menu '{}' not found in registry for restoration", menuId);
            return false;
        }

        if (categoryOpt.isEmpty()) {
            logger.warn("Target category '{}' not found for menu restoration", categoryId);
            return false;
        }

        Menu menu = menuOpt.get();
        Category category = categoryOpt.get();

        // 카테고리에 메뉴가 이미 있는지 확인
        boolean alreadyExists = category.menus().stream()
                .anyMatch(existingMenu -> existingMenu.id().equals(menuId));

        if (alreadyExists) {
            logger.debug("Menu '{}' already exists in category '{}'", menu.name(), category.cateName());
            return true;
        }

        // 카테고리에 메뉴 추가
        List<Menu> updatedMenus = new ArrayList<>(category.menus());
        updatedMenus.add(menu);

        Category updatedCategory = new Category(
                category.cateId(),
                category.cateName(),
                updatedMenus
        );

        try {
            RegistryManager.CATEGORIES.unfreeze();
            RegistryManager.CATEGORIES.add(categoryId, updatedCategory);
        } finally {
            RegistryManager.CATEGORIES.freeze();
        }
        logger.info("Restored menu '{}' to category '{}'", menu.name(), category.cateName());
        return true;
    }

    private void handleUnmappedOrphanMenu(String menuId) {
        Optional<Menu> menuOpt = RegistryManager.MENUS.getById(menuId);
        if (menuOpt.isPresent()) {
            Menu menu = menuOpt.get();
            logger.warn("Could not find original category for orphan menu: '{}' ({})",
                    menu.name(), menuId);
            assignToDefaultCategory(menu);
        }
    }

    private void assignToDefaultCategory(Menu menu) {
        String defaultCategoryId = "uncategorized";
        Optional<Category> defaultCategoryOpt = RegistryManager.CATEGORIES.getById(defaultCategoryId);

        Category defaultCategory;
        if (defaultCategoryOpt.isEmpty()) {
            // 미분류 카테고리 생성
            defaultCategory = new Category(defaultCategoryId, "미분류", new ArrayList<>());
            try {
                RegistryManager.CATEGORIES.unfreeze();
                RegistryManager.CATEGORIES.add(defaultCategoryId, defaultCategory);
            } finally {
                RegistryManager.CATEGORIES.freeze();
            }

            logger.info("Created default category: 미분류");
        } else {
            defaultCategory = defaultCategoryOpt.get();
        }

        // 메뉴를 미분류 카테고리에 추가
        List<Menu> updatedMenus = new ArrayList<>(defaultCategory.menus());
        updatedMenus.add(menu);

        Category updatedCategory = new Category(
                defaultCategory.cateId(),
                defaultCategory.cateName(),
                updatedMenus
        );

        try {
            RegistryManager.CATEGORIES.unfreeze();
            RegistryManager.CATEGORIES.add(defaultCategoryId, updatedCategory);
        } finally {
            RegistryManager.CATEGORIES.freeze();
        }

        logger.info("📁 Assigned orphan menu '{}' to default category '미분류'", menu.name());
    }

    /**
     * rewrited by @biryeongtrain.
     * check at <a href="https://github.com/DEU-CS-Softward-Architecutre-25-1/Cafeteria-Kiosk/pull/24/commits/91d62557278169dc67518ca64b19baab4e427cd4">original code</a>
     *
     *
     * @param menuId target menu
     */
    private void handleMenuDeletion(String menuId) {
        try {
            Optional<Menu> menuToDelete = RegistryManager.MENUS.getById(menuId);
            if (menuToDelete.isEmpty()) {
                logger.warn("Menu not found for deletion: {}", menuId);
                return;
            }

            Menu menu = menuToDelete.get();
            logger.info("Deleting menu: {} ({})", menu.name(), menuId);

            // Registry에서 메뉴 삭제
            try {
                RegistryManager.MENUS.unfreeze();
                boolean menuRemoved = RegistryManager.MENUS.remove(menuId);
                logger.info("Menu removed from registry: {}", menuRemoved);
            } finally {
                RegistryManager.MENUS.freeze();
            }

            // 모든 카테고리에서 해당 메뉴 제거
            boolean isCategoryDirty = false;
            for (Category category : RegistryManager.CATEGORIES.getAll()) {
                List<Menu> menuList = new ArrayList<>(category.menus());
                if (menuList.removeIf(m -> m.id().equals(menuId))) {
                    logger.info("Menu '{}' removed from category '{}'", menu.name(), category.cateName());
                    try {
                        RegistryManager.CATEGORIES.unfreeze();
                        RegistryManager.CATEGORIES.add(category.cateId(), new Category(category.cateId(), category.cateName(), menuList));
                    } finally {
                        RegistryManager.CATEGORIES.freeze();
                    }
                    isCategoryDirty = true;
                }
            }

            // 클라이언트들에게 업데이트된 데이터 전송
            KioskNettyServer server = (KioskNettyServer) handler.connection;

            // 메뉴 레지스트리 전체 전송 (삭제된 메뉴 제외)
            List<SynchronizeData<?>> menuData = new ArrayList<>(RegistryManager.MENUS.getAll());
            server.broadCast(new UpdateDataPacket.ResponseDataS2CPacket(
                    RegistryManager.MENUS.getRegistryId(),
                    menuData));

            // 업데이트된 카테고리들만 전송
            if (isCategoryDirty) {
                List<SynchronizeData<?>> categoryData = new ArrayList<>(RegistryManager.CATEGORIES.getAll());
                server.broadCast(new UpdateDataPacket.ResponseDataS2CPacket(
                        RegistryManager.CATEGORIES.getRegistryId(),
                        categoryData
                ));
            }

            logger.info("Menu deletion completed: {}", menu.name());

        } catch (Exception e) {
            logger.error("Failed to handle menu deletion", e);
        }
    }

    private void handleCategoryDeletion(String categoryId) {
        try {
            backupCurrentMenuCategoryRelations();

            Optional<Category> categoryToDelete = RegistryManager.CATEGORIES.getById(categoryId);
            if (categoryToDelete.isEmpty()) {
                logger.warn("Category not found for deletion: {}", categoryId);
                return;
            }

            Category category = categoryToDelete.get();
            logger.info("Deleting category: {} ({})", category.cateName(), categoryId);
            try {
                RegistryManager.MENUS.unfreeze();
                // 카테고리에 속한 메뉴들 먼저 삭제
                for (Menu menu : category.menus()) {
                    RegistryManager.MENUS.remove(menu.id());
                    logger.info("Deleted menu: {} from category deletion", menu.name());
                }
            } finally {
                RegistryManager.MENUS.freeze();
            }
            // 카테고리 삭제
            try {
                RegistryManager.CATEGORIES.unfreeze();
                boolean categoryRemoved = RegistryManager.CATEGORIES.remove(categoryId);
                logger.info("Category removed from registry: {}", categoryRemoved);
            } finally {
                RegistryManager.CATEGORIES.freeze();
            }

            // 클라이언트들에게 업데이트 전송
            KioskNettyServer server = (KioskNettyServer) handler.connection;

            List<SynchronizeData<?>> menuData = new ArrayList<>(RegistryManager.MENUS.getAll());
            List<SynchronizeData<?>> categoryData = new ArrayList<>(RegistryManager.CATEGORIES.getAll());

            server.broadCast(new UpdateDataPacket.ResponseDataS2CPacket(
                    RegistryManager.MENUS.getRegistryId(),
                    menuData
            ));
            server.broadCast(new UpdateDataPacket.ResponseDataS2CPacket(
                    RegistryManager.CATEGORIES.getRegistryId(),
                    categoryData
            ));


            logger.info("Category deletion completed: {}", category.cateName());

        } catch (Exception e) {
            logger.error("Failed to handle category deletion", e);
            try {
                logger.info("Attempting to restore orphan menus due to deletion error...");
                restoreOrphanMenusFromBackup();
            } catch (Exception restoreException) {
                logger.error("Failed to restore orphan menus from backup", restoreException);
            }
        }
    }

    @Override
    public void onRequestVerify(VerifyPurchasePackets.VerifyPurchasePacketC2S packet) {
        if (!this.handler.isEncrypted()) {
            throw new IllegalStateException("Client is not encrypted");
        }
        // TODO IMPLEMENT PACKET HANDLING
    }

    @Override
    public SidedPacket.Side getSide() {
        return SidedPacket.Side.SERVER;
    }

    @Override
    public SerializableHandler getHandler() {
        return this.handler;
    }
}
