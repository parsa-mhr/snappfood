package org.example.ApiHandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.hibernate.SessionFactory;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantDispatcher برای هدایت درخواست‌های مرتبط با رستوران‌ها
 */
public class RestaurantDispatcher implements HttpHandler {
    private final SessionFactory sessionFactory;

    public RestaurantDispatcher(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");

            // بررسی مسیرهای معتبر
            if (segments.length < 2 || !segments[1].equals("restaurants")) {
                sendJson(exchange, 400, jsonError("مسیر نامعتبر است"));
                return;
            }

            // هدایت درخواست‌ها
             if (segments.length == 2) {
                 if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                     new RestaurantsApiHandler(sessionFactory).handle(exchange);
                 }
             }
            if (segments.length == 4 && segments[3].equals("orders")) {
                // برای GET /restaurants/{id}/orders
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    new RestaurantOrdersApiHandler(sessionFactory).handle(exchange);
                } else {
                    sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                }
            } else if (segments.length == 4 && segments[2].equals("orders")) {
                // برای PATCH /restaurants/orders/{order_id}
                if (exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
                    new RestaurantOrderStatusUpdateApiHandler(sessionFactory).handle(exchange);
                } else {
                    sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                }

            } else if (path.equals("/restaurants/mine")) {
                new MineApiHandler(sessionFactory).handle(exchange);
            } else if (segments.length >= 3 && segments[1].equals("restaurants")) {
                Long restaurantId = Long.parseLong(segments[2]);
                if (segments.length == 3) {
                    // برای PUT /restaurants/{id}
                    if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                        new RestaurantsUpdateApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                } else if (segments.length == 4 && segments[3].equals("item")) {
                    // برای POST /restaurants/{id}/item
                    if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        new RestaurantsItemApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                }else if (segments.length == 4 && segments[3].equals("items")) {
                    // برای GET /restaurants/{id}/item
                    if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        new RestaurantGetMenuItemsApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                }
                else if (segments.length == 4 && segments[3].equals("menu")) {
                    // برای POST /restaurants/{id}/menu
                    if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        new RestaurantMenuApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                }else if (segments.length == 4 && segments[3].equals("menus")) {
                // برای GET /restaurants/{id}/menus
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    new RestaurantGetMenuApiHandler(sessionFactory).handle(exchange);
                } else {
                    SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                }
            }

                else if (segments.length == 4 && segments[3].equals("orders")) {
                    // برای GET /restaurants/{id}/orders
                    if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        new RestaurantOrdersApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                } else if (segments.length == 5 && segments[3].equals("menu")) {
                    // برای PUT /restaurants/{id}/menu/{title} و DELETE /restaurants/{id}/menu/{title}
                    if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                        new RestaurantMenuAddItemApiHandler(sessionFactory).handle(exchange);
                    } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                        new RestaurantMenuDeleteApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                } else if (segments.length == 5 && segments[3].equals("item")) {
                    // برای PUT و DELETE /restaurants/{id}/item/{item_id}
                    if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                        new RestaurantsUpdateItemApiHandler(sessionFactory).handle(exchange);
                    } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                        new RestaurantMenuItemDeleteApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                } else if (segments.length == 6 && segments[3].equals("menu")) {
                    // برای DELETE /restaurants/{id}/menu/{title}/{item_id}
                    if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                        new RestaurantMenuRemoveItemApiHandler(sessionFactory).handle(exchange);
                    } else {
                        SendJson.sendJson(exchange, 405, SendJson.jsonError("متد مجاز نیست"));
                    }
                } else {
                    SendJson.sendJson(exchange, 400, SendJson.jsonError("مسیر نامعتبر است"));
                }
            } else {
                SendJson.sendJson(exchange, 400, SendJson.jsonError("مسیر نامعتبر است"));}
//            } else if (segments.length == 2) {
//                if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
//                    new RestaurantsApiHandler(sessionFactory).handle(exchange);
//                }
//            }else if (segments.length == 3 && segments[2].equals("mine")) {
//                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
//                    new MineApiHandler(sessionFactory).handle(exchange);
//                }
//            }else if (segments.length == 3) {
//                if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
//                    new RestaurantsUpdateApiHandler(sessionFactory).handle(exchange);
//                }
//            }else if (segments.length == 4 && segments[3].equals("item")) {
//                if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
//                    new RestaurantsItemApiHandler(sessionFactory).handle(exchange);
//                }
//            }else if (segments.length == 5 && segments[3].equals("item")) {
//                if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
//                    new RestaurantsUpdateItemApiHandler(sessionFactory).handle(exchange);
//                }else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
//                    //delete handler
//                }
            // }

//
//            else {
//                sendJson(exchange, 400, jsonError("مسیر نامعتبر است"));
//            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
