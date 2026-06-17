# FoodExpress — Order Service

> **Texnologiya:** Java / Spring Boot · PostgreSQL (`order_db`) · RabbitMQ · Port `8081`
> **Markaziy hujjatlar:** [foodexpress-docs](https://github.com/javlonbek16/foodexpress-docs)

## 1. Servis maqsadi

Buyurtma hayotiy siklining markazi: savatcha, buyurtma yaratish, **holat o'tishlari**, kuryer biriktirish. Har muhim o'zgarishda **RabbitMQ event** chiqaradi.

## 2. Talablar (intern nima qilishi kerak)

### Funksional
- [ ] Savatcha: qo'shish/ko'rish/o'chirish (`order.create` permission).
- [ ] Buyurtma yaratish (`POST /orders`) → DB'ga yozadi + `order.created` event chiqaradi.
- [ ] Buyurtmalarni ko'rish: rolga qarab filtr — CUSTOMER faqat o'zinikini, RESTAURANT o'z restoranidagini, COURIER o'ziga biriktirilganini, ADMIN hammasini.
- [ ] Holat o'zgartirish (`PATCH /orders/{id}/status`) → `order.status_changed` event. Faqat ruxsat etilgan o'tishlar (status machine).
- [ ] Kuryer biriktirish (`POST /orders/{id}/assign-courier`).
- [ ] Buyurtma yaratishda Restaurant servisдан menu item / narxni **REST orqali** tekshirish.

### Texnik
- [ ] JWT'ni **lokal** tekshirish (Auth'ga bormaydi) — `JWT_SECRET`, `permissions[]`. Qarang [RBAC.md](https://github.com/javlonbek16/foodexpress-docs/blob/main/RBAC.md).
- [ ] RabbitMQ event formati **aniq** [EVENTS.md](https://github.com/javlonbek16/foodexpress-docs/blob/main/EVENTS.md) dagidek (exchange `foodexpress`, topic).
- [ ] Status machine: `CREATED→CONFIRMED→PREPARING→READY→DELIVERING→DELIVERED`, + `CANCELLED`. Noto'g'ri o'tish → `409`.
- [ ] Swagger: `http://localhost:8081/swagger-ui.html`. CORS yoqilgan.

## 3. Ma'lumotlar modeli (`order_db`)

**orders** — `id` (UUID), `customer_id`, `restaurant_id`, `courier_id` (nullable), `status`, `total_price`, `currency`, `created_at`, `updated_at`
**order_items** — `id`, `order_id` (FK), `menu_item_id`, `name`, `qty`, `price`
**cart** — `id`, `customer_id`
**cart_items** — `id`, `cart_id` (FK), `menu_item_id`, `name`, `qty`, `price`
**order_status_history** — `id`, `order_id`, `old_status`, `new_status`, `changed_at`, `changed_by`

> `restaurant_id`, `menu_item_id` — Restaurant servisдаги ID'lar (FK emas, faqat reference; baza alohida).

## 4. Asosiy endpointlar

| Method | Path | Permission |
|---|---|---|
| POST | `/cart/items` | `order.create` |
| GET | `/cart` | `order.create` |
| POST | `/orders` | `order.create` |
| GET | `/orders` | `order.read.own` / `order.read.all` |
| GET | `/orders/{id}` | `order.read.own` / `order.read.all` |
| PATCH | `/orders/{id}/status` | `order.update.status` |
| POST | `/orders/{id}/assign-courier` | `order.update.status` |

## 5. Acceptance criteria
- ✅ Buyurtma yaratilganda DB'ga yoziladi VA `order.created` event chiqadi (RabbitMQ UI'da ko'rinadi).
- ✅ Har holat o'zgarishi `order.status_changed` event chiqaradi va `order_status_history` ga yoziladi.
- ✅ Ruxsat etilmagan status o'tishi `409` qaytaradi.
- ✅ CUSTOMER boshqa mijoz buyurtmasini ko'ra olmaydi (own filtr ishlaydi).
- ✅ Event payloadlari EVENTS.md ga to'liq mos.

## 6. Arxitektura chegaralari
- ❌ API Gateway yo'q. ❌ Boshqa baza yo'q. ✅ Faqat lokal.

## 7. O'rganish maqsadi
Status machine, event-driven arxitektura (publisher), servislararo REST chaqiruv, JWT lokal tekshiruv, ma'lumot egaligi (ownership) bo'yicha filtrlash.
