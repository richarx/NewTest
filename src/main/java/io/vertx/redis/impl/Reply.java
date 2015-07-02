package io.vertx.redis.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class Reply {

  private final byte type;
  private final Object data;

  public Reply(byte type, Object data) {
    this.type = type;
    this.data = data;
  }

  public Reply(byte type, int size) {
    this.type = type;
    this.data = new Reply[size];
  }

  void set(int pos, Reply reply) {
    ((Reply[]) data)[pos] = reply;
  }

  public boolean is(byte b) {
    return type == b;
  }

  public boolean is(char b) {
    return type == (byte) b;
  }

  /**
   * Return the type of instance of this Reply. Useful to avoid checks against instanceof
   *
   * @return enum
   */
  byte type() {
    return type;
  }

  public Object data() {
    return data;
  }

  @SuppressWarnings("unchecked")
  public <T> T asType(final Class<T> type, final String encoding) throws ClassCastException {

    if (data == null) {
      return null;
    }

    if (type == String.class) {
      if (data instanceof String) {
        return (T) data;
      }
      if (data instanceof Buffer) {
        return (T) ((Buffer) data).toString(encoding);
      }
      return (T) data.toString();
    }

    if (type == Long.class) {
      return (T) data;
    }

    if (type == Void.class) {
      return null;
    }

    if (type == JsonArray.class) {
      final JsonArray multi = new JsonArray();

      for (Reply r : (Reply[]) data) {
        Object elem;
        switch (r.type()) {
          case '+':  
          case '$':   // Bulk
            elem = r.asType(String.class, encoding);
            break;
          case ':':   // Integer
            elem = r.asType(Long.class, encoding);
            break;
          case '*':   // Multi
            elem = r.asType(JsonArray.class, encoding);
            break;
          default:
            throw new RuntimeException("Unknown sub message type in multi: " + r.type());
        }
        if (elem == null) {
          multi.addNull();
        } else {
          multi.add(elem);
        }
      }

      return (T) multi;

    }

    if (type == JsonObject.class) {
      final JsonObject multi = new JsonObject();

      for (int i = 0; i < ((Reply[]) data).length; i += 2) {
        if (((Reply[]) data)[i].type() != '$') {
          throw new RuntimeException("Expected String as key type in multi: " + ((Reply[]) data)[i].type());
        }

        Reply brKey = ((Reply[]) data)[i];
        Reply brValue = ((Reply[]) data)[i + 1];

        switch (brValue.type()) {
          case '$':   // Bulk
            multi.put(brKey.asType(String.class, encoding), brValue.asType(String.class, encoding));
            break;
          case ':':   // Integer
            multi.put(brKey.asType(String.class, encoding), brValue.asType(Long.class, encoding));
            break;
          case '*':   // Multi
            multi.put(brKey.asType(String.class, encoding), brValue.asType(JsonArray.class, encoding));
            break;
          default:
            throw new RuntimeException("Unknown sub message type in multi: " + ((Reply[]) data)[i + 1].type());
        }

      }
      return (T) multi;
    }

    return null;
  }

  public <T> T asType(Class<T> type) throws ClassCastException {
    return asType(type, "UTF-8");
  }
}
