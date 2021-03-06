package com.example.myapplicati.ui.main;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplicati.R;
import com.example.myapplicati.db.AppDatabase;
import com.example.myapplicati.db.AppPref;
import com.example.myapplicati.db.model.CartItem;
import com.example.myapplicati.helper.GridSpacingItemDecoration;
import com.example.myapplicati.helper.Utils;
import com.example.myapplicati.networking.model.Product;
import com.example.myapplicati.ui.base.BaseActivity;
import com.example.myapplicati.ui.cart.CartBottomSheetFragment;
import com.example.myapplicati.ui.splash.SplashActivity;
import com.example.myapplicati.ui.transactions.TransactionsActivity;
import com.example.myapplicati.ui.views.CartInfoBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements ProductsAdapter.ProductsAdapterListener {
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.cart_info_bar)
    CartInfoBar cartInfoBar;

    private ProductsAdapter mAdapter;
    private Realm realm;
    private RealmResults<CartItem> cartItems;
    private RealmChangeListener<RealmResults<CartItem>> cartRealmChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        changeStatusBarColor();

        init();
        renderProducts();

        realm = Realm.getDefaultInstance();
        cartItems = realm.where(CartItem.class).findAllAsync();

        cartRealmChangeListener = cartItems -> {
            Timber.d("Cart items changed! " + this.cartItems.size());
            if (cartItems != null && cartItems.size() > 0) {
                setCartInfoBar(cartItems);
                toggleCartBar(true);
            } else {
                toggleCartBar(false);
            }

            mAdapter.setCartItems(cartItems);
        };
    }

    private void setCartInfoBar(RealmResults<CartItem> cartItems) {
        int itemCount = 0;
        for (CartItem cartItem : cartItems) {
            itemCount += cartItem.quantity;
        }
        cartInfoBar.setData(itemCount, String.valueOf(Utils.getCartPrice(cartItems)));
    }

    /**
     * Rendering the products from local db
     */
    private void renderProducts() {
        RealmResults<Product> products = AppDatabase.getProducts();
        mAdapter = new ProductsAdapter(this, products, this);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void init() {
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        cartInfoBar.setListener(() -> showCart());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.transactions) {
            startActivity(new Intent(MainActivity.this, TransactionsActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.clear_cart) {
            AppDatabase.clearCart();
        }

        if (item.getItemId() == R.id.logout) {
            AppDatabase.clearData();
            AppPref.getInstance().clearData();
            launchLogin();
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchLogin() {
        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    void showCart() {
        CartBottomSheetFragment fragment = new CartBottomSheetFragment();
        fragment.show(getSupportFragmentManager(), fragment.getTag());
    }

    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    @Override
    public void onProductAddedCart(int index, Product product) {
        AppDatabase.addItemToCart(product);
        if (cartItems != null) {
            mAdapter.updateItem(index, cartItems);
        }
    }

    @Override
    public void onProductRemovedFromCart(int index, Product product) {
        AppDatabase.removeCartItem(product);
        if (cartItems != null) {
            mAdapter.updateItem(index, cartItems);
        }
    }

    private void toggleCartBar(boolean show) {
        if (show)
            cartInfoBar.setVisibility(View.VISIBLE);
        else
            cartInfoBar.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cartItems != null) {
            // cartItems.removeChangeListener(cartRealmChangeListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        checkSession(MainActivity.this);
        if (cartItems != null) {
            cartItems.addChangeListener(cartRealmChangeListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartItems != null) {
            cartItems.addChangeListener(cartRealmChangeListener);
        }
        if (realm != null) {
            realm.close();
        }
    }
}
